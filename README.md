# 그랑블루 판타지 갤러리 검색기 백엔드
## 개요
### [->사이트로 이동](http://spiaminto.ap-northeast-2.elasticbeanstalk.com/spiachat/lobby)

그랑블루 판타지 갤러리의 일부 글을 유사도 기반으로 검색할 수 있는 서비스 '그랑블루 판타지 갤러리 검색기' 의 백엔드 입니다.<br>
그랑블루 갤러리에서의 글 스크래핑, OPENAI text embedding 서비스를 이용한 임베딩 등을 수행합니다.<br>

## 1. 사용 기술
Java 17, 
SpringBoot 3, Spring AI 1.0.0, Spring Data Jpa,
Selenium, Jsoup,
PostgresSql

## 1. 스크래핑

Selenium 을 이용하여 그랑블루 판타지 갤러리의 글을 스크래핑 합니다.<br>
Jsoup 파싱과 explicit Timeout 을 사용해 스크래핑 속도를 최대한 올렸습니다.<br>
스크래핑된 글은 비동기 방식으로 DB 에 저장되며, DB 저장시 BulkInsert 를 사용하여 DB 요청 수를 줄이고, 속도를 올렸습니다.<br>

## 2. 임베딩

SpringAI 의 openai 모듈을 이용하여 글을 임베딩 합니다.
글은 DB 에서 페이지 단위로 가져와 비동기 방식으로 임베딩 되며 openai 의 text-embedding-large 모델을 사용하여 256차원으로 임베딩 됩니다.
임베딩 된 글 역시 BulkInsert 방식으로 DB 에 저장됩니다.
임베딩 값은 hnsw 방식으로 인덱싱 되어있습니다.

