# ==================================================================
# kidhack-backend 런타임 이미지
#
# jar 는 이 안에서 빌드하지 않고, CI 가 미리 만들어 둔 것을 가져온다.
#   ./gradlew bootJar && docker build -t jotriever/kidhack-was:local .
#
# Dockerfile 안에서 gradle 을 돌리지 않는 이유:
#   테스트와 빌드가 GitHub Actions 에서 이미 끝나고 Gradle 캐시가 그대로
#   먹히므로, 여기서 다시 빌드하면 같은 일을 두 번 하면서 캐시만 잃는다.
# ==================================================================
FROM eclipse-temurin:17-jre-jammy

# ------------------------------------------------------------------
# DocumentDB TLS 트러스트스토어
#
# 원래 이 작업은 WAS 의 user_data 가 부팅할 때마다 했다. 컨테이너로 옮기면
# 호스트가 아니라 "이 이미지 안의 JDK" 를 기준으로 만들어야 하므로 여기로 온다.
# 덕분에 로컬에서도 운영과 똑같은 트러스트스토어로 재현된다.
#
# DocumentDB 인증서는 Amazon RDS CA(프라이빗 CA)가 발급하므로 JDK 기본
# cacerts 에 없다. 그렇다고 RDS 번들만 담은 새 트러스트스토어를 만들면
# 퍼블릭 CA 가 전부 사라져 ElastiCache(Redis) TLS 접속이 깨진다.
# → 반드시 "기본 cacerts 를 복사한 뒤" 거기에 RDS CA 를 추가한다.
#
# 또 global-bundle.pem 에는 인증서가 수십 개 들어 있는데 keytool 은 첫 번째
# 것만 가져오므로, csplit 으로 쪼개 하나씩 넣어야 한다.
# ------------------------------------------------------------------
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl; \
    mkdir -p /opt/certs; \
    cp "$JAVA_HOME/lib/security/cacerts" /opt/certs/truststore.jks; \
    curl -sSf -o /tmp/global-bundle.pem \
        https://truststore.pki.rds.amazonaws.com/global/global-bundle.pem; \
    cd /tmp; \
    csplit -sz -f rds-cert- global-bundle.pem '/-BEGIN CERTIFICATE-/' '{*}'; \
    for CERT in rds-cert-*; do \
        keytool -importcert -noprompt -trustcacerts \
            -alias "rds-$CERT" -file "$CERT" \
            -keystore /opt/certs/truststore.jks -storepass changeit; \
    done; \
    chmod 644 /opt/certs/truststore.jks; \
    rm -rf /tmp/rds-cert-* /tmp/global-bundle.pem /var/lib/apt/lists/*

# 예전에는 /opt/app/app.env 에 있던 값. 트러스트스토어 경로가 이미지 안으로
# 바뀌었으므로 이미지가 스스로 들고 있는 편이 맞다.
# (인프라 쪽 app.env 에서는 이 줄을 제거한다)
ENV JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=/opt/certs/truststore.jks -Djavax.net.ssl.trustStorePassword=changeit"

# 컨테이너 안에서 root 로 돌 이유가 없다.
# 그룹을 root(0)로 두는 것은 OpenShift 등 임의 UID 환경 호환을 위한 관례다.
RUN useradd -r -u 10001 -g root appuser

WORKDIR /app

# ⚠️ -plain.jar 를 넣으면 "no main manifest attribute" 로 죽는다.
#    버전을 올리면 이 줄과 아래 ARG 를 함께 고칠 것.
ARG JAR_FILE=build/libs/kidhack-backend-0.0.1-SNAPSHOT.jar
COPY --chown=10001:root ${JAR_FILE} /app/app.jar

USER 10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]