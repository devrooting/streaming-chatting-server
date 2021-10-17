# streaming-chatting-server
라이브 스트리밍 및 채팅 기능

### 기술 스택
- RTMP/HLS/DASH
- WebSocket
- Netty
- MariaDB
- Nginx

### 서버 구축 (nginx-rtmp 모듈 컴파일 설치)

```sh
# 관련 라이브러리 및 유틸 설치
~$ > wget http://www.openssl.org/source/openssl-1.1.1g.tar.gz
~$ > tar xvf openssl-1.1.1g.tar.gz

~$ > wget https://acc.dl.osdn.jp/sfnet/p/pc/pcre/pcre/8.32/pcre-8.32.tar.gz
~$ > tar xvf pcre-8.32.tar.gz
~$ > cd pcre-8.32/
~$ > ./configure --prefix=/usr/local
~$ > make
~$ > sudo make install

~$ > ../
~$ > wget https://zlib.net/fossils/zlib-1.2.8.tar.gz
~$ > tar xvf zlib-1.2.8.tar.gz
~$ > cd zlib-1.2.8/
~$ > ./configure --prefix=/usr/localmake
~$ > make -j4
~$ > sudo make install

~$ > ../
~$ > sudo apt-get install build-essential libpcre3 libpcre3-dev libssl-dev zlib1g-dev

# nginx 다운
~$ > sudo wget http://nginx.org/download/nginx-1.14.0.tar.gz
~$ > sudo tar xzf nginx-1.14.0.tar.gz
~$ > cd nginx-1.14.0

# nginx-rtmp-module 설치
~$ > sudo git clone git://github.com/arut/nginx-rtmp-module.git

# 환경설정
~$ > ./configure --with-http_ssl_module --add-module=../nginx-rtmp-module 
~$ > make 
~$ > sudo make install
```

### 자바 개발환경 셋팅

```sh
# java 설치
~$ > sudo apt-get install openjdk-8-jdk

# 설치된 자바 경로 확인
~$ > which javac #/usr/bin/javac
~$ > readlink -f /usr/bin/javac #/usr/lib/jvm/java-8-openjdk-amd64/bin/javac

# java 환경변수 지정
~$ > sudo nano /etc/profile

# java 경로 입력 후 저장
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export CLASS_PATH=$JAVA_HOME/lib:$CLASS_PATH

# 변경사항 저장 후 확인
~$ > source /etc/profile
~$ > echo $JAVA_HOME
```
