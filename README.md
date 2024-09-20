# Github 마이너 갤러리 검색기 백엔드
## 개요

Github 마이너 갤러리의 일부 글을 유사도 기반으로 검색할 수 있는 서비스 'Github 마이너 갤러리 검색기' 의 백엔드 입니다.<br>
Github 갤러리의 글 스크래핑 및 저장, OPENAI text embedding 서비스를 이용한 임베딩 등을 수행합니다.<br>

## 사용 기술
Java 17,  
SpringBoot 3, Spring AI 1.0.0, Spring Data Jpa,  
Playwright, Jsoup,  
PostgreSQL

## 1. 스크래핑
<div style="text-align:center"><img src="https://github.com/user-attachments/assets/4f57cd0c-95cd-4876-862e-f67340676fa1" width="90%"></div>

Playwright 를 이용하여 Github 갤러리의 글을 스크래핑 합니다.  
Playwright 으로 페이지 접속 후 필요한 요소를 찾아 Jsoup 으로 파싱하여 글을 가져옵니다.  

<div style="text-align:center"><img src="https://github.com/user-attachments/assets/e6812726-8e29-4e9a-be4f-20ad46882ed9" width="90%"></div>

약 1.5초당 1건의 글을 스크래핑 하며, 디시측의 응답에 문제가 없는 한 모든 글을 안정적으로 수집합니다.  
스크래핑된 글은 스크래핑 진행중에 비동기 방식으로 DB 에 저장됩니다.<br>
DB 저장시 jdbcTemplate.batchUpdate 를 사용하여 (인터벌 * 100)개의 데이터를 한꺼번에 삽입합니다.<br>

## 2. 중복관리
<img src="https://github.com/user-attachments/assets/1e77c3d9-29e7-453c-afc2-bf287a2280f0" width="50%">

페이지 단위로 스크래핑하기 때문에 중복된 글과 댓글을 수집할 수 있습니다.  
해당 중복 글과 댓글을 임베딩 전에 미리 조회하고 삭제하는 기능입니다.  
수십만건의 데이터에서 중복 데이터를 찾고 지우는 작업이 최대한 빠르게 수행 되도록 쿼리를 최적화 하였습니다.

## 3. 임베딩
<img src="https://github.com/user-attachments/assets/47a44b0f-ee58-454a-b1c6-516a717daa78" width="90%">

SpringAI 의 openai 모듈을 이용하여 글을 임베딩 합니다.  
글은 DB 에서 페이지 단위로 가져와 비동기 방식으로 임베딩 되며 openai 의 text-embedding-large 모델을 사용하여 256차원으로 임베딩 됩니다.
임베딩 결과값은 jdbcTemplate.batchUpdate 방식으로 DB 에 저장됩니다.  
DB 는 postgreSQL 에 vector 확장을 사용하였습니다.

<img src="https://github.com/user-attachments/assets/8e56fe15-3e6c-4e1f-bea3-716b0cdeaaeb" width="90%">

임베딩 작업은 멀티스레드로 진행되며, 100개씩 묶어 1000 개를 진행할 경우 1분 20초 정도 소요됩니다.   
임베딩 값은 hnsw (코사인 거리) 방식으로 인덱싱 되어있습니다.

## 4. 페이지 찾기
<img src="https://github.com/user-attachments/assets/755fad11-4bac-4431-b80b-7b9ffad4c027" width="90%">

필요한 경우 날짜를 기준으로 해당 날짜의 페이지를 찾아주는 기능입니다. (오차 약 1페이지)   
특정 날짜를 기준으로 스크래핑 할때 사용합니다.

