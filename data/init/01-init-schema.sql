-- PostgreSQL 초기화 스크립트
-- account 스키마 생성 및 권한 설정

-- account 스키마 생성
CREATE SCHEMA IF NOT EXISTS account;

-- postgres 사용자에게 account 스키마 권한 부여
GRANT ALL PRIVILEGES ON SCHEMA account TO postgres;

-- 기본 검색 경로를 account 스키마로 설정
ALTER USER postgres SET search_path TO account, public;

-- 현재 세션의 검색 경로도 설정
SET search_path TO account, public;
