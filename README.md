# 그랑블루 판타지 갤러리 검색기 백엔드
## 개요

그랑블루 판타지 갤러리의 일부 글을 유사도 기반으로 검색할 수 있는 서비스 '그랑블루 판타지 갤러리 검색기' 의 백엔드 입니다.<br>
그랑블루 갤러리에서의 글 스크래핑, OPENAI text embedding 서비스를 이용한 임베딩 등을 수행합니다.<br>

## 사용 기술
Java 17,  
SpringBoot 3, Spring AI 1.0.0, Spring Data Jpa,  
Selenium, Jsoup,  
PostgreSQL

## 1. 스크래핑
<div style="text-align:center"><img src="https://github.com/user-attachments/assets/5dfb96a8-647b-4359-98ed-43df7859a935" width="90%"></div>

Selenium 을 이용하여 그랑블루 판타지 갤러리의 글을 스크래핑 합니다. (실제 사용은 headless 상태로 사용합니다.)<br>
Selenium 으로 페이지 접속 후 Jsoup 으로 필요한 요소를 파싱하여 글을 가져옵니다.<br>

<div style="text-align:center"><img src="https://github.com/user-attachments/assets/14c26236-340f-4d4e-89eb-9bdc7c42b3ab" width="90%"></div>
<div style="text-align:center"><img src="https://github.com/user-attachments/assets/abe7f14e-1dc9-4785-a9db-be47df08141b" width="90%"></div>

문제가 발생하지 않는 선에서, 최대한 빠른 속도로 스크래핑 하도록 하였습니다. (1 ~ 2 초당 1페이지)<br>
스크래핑된 글은 스크래핑 진행중에 비동기 방식으로 DB 에 저장됩니다.<br>
DB 저장시 jdbcTemplate.batchUpdate 를 사용하여 (인터벌 * 100) 개의 데이터를 한번에 삽입합니다.<br>

## 2. 중복관리
<img src="https://github.com/user-attachments/assets/0b0a7fdb-2f4b-4a8c-9882-52a7d5a68397" width="50%">

페이지 단위로 스크래핑 함에 따라 중복 글과 댓글이 발생할 수 있습니다.<br>
해당 중복 글과 댓글을 임베딩 전에 미리 조회하고 삭제하는 기능입니다.

## 3. 임베딩
<img src="https://github.com/user-attachments/assets/8586d607-d3f3-4b54-99cb-436143db4404" width="90%">

SpringAI 의 openai 모듈을 이용하여 글을 임베딩 합니다.  
글은 DB 에서 페이지 단위로 가져와 비동기 방식으로 임베딩 되며 openai 의 text-embedding-large 모델을 사용하여 256차원으로 임베딩 됩니다.
임베딩 결과값은 jdbcTemplate.batchUpdate 방식으로 DB 에 저장됩니다.  
DB 는 postgreSQL 에 vector 확장을 사용하였습니다.

<img src="https://github.com/user-attachments/assets/84a5f021-131b-4fea-a908-f6973b40312d" width="90%">

임베딩 작업은 멀티스레드로 진행되며, 100개씩 묶어 1000 개를 진행할 경우 1분 2초 정도 소요됩니다.   
임베딩 값은 hnsw (코사인 거리) 방식으로 인덱싱 되어있습니다.

## 4. 페이지 찾기
<img src="https://github.com/user-attachments/assets/f36eb248-4f0a-4b8a-8a26-cb42a15ce397" width="90%">

필요한 경우 날짜를 기준으로 해당 날짜의 페이지를 찾아주는 기능입니다. (오차 약 1페이지)   
특정 날짜를 기준으로 스크래핑 할때 사용합니다.

