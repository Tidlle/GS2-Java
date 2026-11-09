# Imagem da Zela API (build multi-stage).
# Build a partir da RAIZ do repositório:
#   docker build -f dockerfiles/Dockerfile -t zela-api .

# --- Etapa 1: compila e gera o .jar ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Baixa as dependências primeiro para aproveitar o cache das camadas.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
# Os testes rodam no pipeline de CI; aqui só empacotamos.
RUN ./mvnw -q -B package -DskipTests

# --- Etapa 2: imagem final só com o JRE ---
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuário sem privilégios de root.
RUN groupadd --system zela && useradd --system --gid zela --create-home zela \
    && mkdir -p /app/uploads && chown -R zela:zela /app
USER zela

COPY --from=build --chown=zela:zela /app/target/zela-api-*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
