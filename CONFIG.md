# Configurar Github Action

## Añadir el .github/workflows/workflow.yml

Con configuración para ejecutar el análisis de SonarQube en cada push a master o develop, o en cada pull request en self-hosted SonarQube:
```yaml
name: SonarQube Analysis
on:
  push:
    branches: [master, develop]
    pull_request:
      types: [opened, synchronize, reopened]

jobs:
  sonarqube:
    runs-on: self-hosted
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: SonarQube Scan
        run: mvn clean verify sonar:sonar -Dsonar.host.url=http://sonarqube.internal:9000 -Dsonar.token=${{ secrets.SONAR_TOKEN }}
```

## Descargar e instalar el agente de Github Actions en el servidor donde se encuentra alojado SonarQube

Siguiendo los pasos de la [documentación oficial](https://docs.github.com/en/actions/hosting-your-own-runners/managing-self-hosted-runners/adding-self-hosted-runners).

- En tu repositorio de GitHub: **Settings → Actions → Runners → New self-hosted runner**
- GitHub te dará los comandos para instalar el agente.

# Docker 

## Docker Compose
```yaml
# =============================================================================
# SonarQube Server + PostgreSQL
# =============================================================================
# Uso:
#   Levantar:   docker-compose up -d
#   Detener:    docker-compose down
#   Ver logs:   docker-compose logs -f sonarqube
#   Destruir:   docker-compose down -v  (elimina volúmenes y datos)
#
# Acceso:
#   URL:        http://localhost:9000
#   Usuario:    admin
#   Password:   admin  (se pedirá cambiarla en el primer login)
# =============================================================================

services:

  # ---------------------------------------------------------------------------
  # Base de datos PostgreSQL (recomendada por SonarSource para producción)
  # ---------------------------------------------------------------------------
  sonarqube-db:
    image: postgres:15-alpine
    container_name: sonarqube-db
    restart: unless-stopped
    environment:
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar_password    # Cambiar en producción
      POSTGRES_DB: sonarqube
    volumes:
      - sonarqube_db_data:/var/lib/postgresql/data
    networks:
      - sonarqube-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U sonar -d sonarqube"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ---------------------------------------------------------------------------
  # SonarQube Server (Community Edition)
  # ---------------------------------------------------------------------------
  sonarqube:
    image: sonarqube:10-community
    container_name: sonarqube
    restart: unless-stopped
    depends_on:
      sonarqube-db:
        condition: service_healthy
    environment:
      # Conexión a la base de datos
      SONAR_JDBC_URL: jdbc:postgresql://sonarqube-db:5432/sonarqube
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar_password   # Debe coincidir con POSTGRES_PASSWORD

      # Configuración de memoria (ajustar según recursos del servidor)
      SONAR_CE_JAVAOPTS: "-Xmx1g -Xms512m"
      SONAR_WEB_JAVAOPTS: "-Xmx1g -Xms512m"
      SONAR_SEARCH_JAVAOPTS: "-Xmx1g -Xms512m"

      # Opcional: forzar autenticación (recomendado)
      # SONAR_FORCEAUTHENTICATION: "true"
    ports:
      - "9000:9000"     # UI y API de SonarQube
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions
      - sonarqube_logs:/opt/sonarqube/logs
    networks:
      - sonarqube-net
    # -------------------------------------------------------------------------
    # IMPORTANTE: SonarQube usa Elasticsearch internamente, que requiere
    # vm.max_map_count >= 262144 en el host.
    #
    # Ejecutar en el host ANTES de levantar el compose:
    #   sudo sysctl -w vm.max_map_count=262144
    #
    # Para hacerlo permanente, agregar al archivo /etc/sysctl.conf:
    #   vm.max_map_count=262144
    # -------------------------------------------------------------------------
    ulimits:
      nofile:
        soft: 131072
        hard: 131072
      nproc:
        soft: 8192
        hard: 8192

# -----------------------------------------------------------------------------
# Volúmenes persistentes
# -----------------------------------------------------------------------------
volumes:
  sonarqube_db_data:
    driver: local
  sonarqube_data:
    driver: local
  sonarqube_extensions:
    driver: local
  sonarqube_logs:
    driver: local

# -----------------------------------------------------------------------------
# Red interna
# -----------------------------------------------------------------------------
networks:
  sonarqube-net:
    driver: bridge
```	

## Configuración del servidor
```bash	
# Elasticsearch (embebido en SonarQube) requiere este parámetro
sudo sysctl -w vm.max_map_count=262144

# Para hacerlo permanente:
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```	
## Levantar el servidor
```bash
docker-compose -f docker-compose.yml up -d
```

# Ejecución

## Ejecutar análisis de SonarQube con Maven
```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=your_sonar_token
```


## Generar token
```bash
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=quality-measurement \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=sqp_c146996a60ad6a698cee8236f917f0c193668288
```

## Configuración del runner
```bash
# Create a folder
$ mkdir actions-runner && cd actions-runner

# Download the latest runner package
$ curl -o actions-runner-linux-x64-2.331.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.331.0/actions-runner-linux-x64-2.331.0.tar.gz

# Optional: Validate the hash
$ echo "5fcc01bd546ba5c3f1291c2803658ebd3cedb3836489eda3be357d41bfcf28a7  actions-runner-linux-x64-2.331.0.tar.gz" | shasum -a 256 -c

# Extract the installer
$ tar xzf ./actions-runner-linux-x64-2.331.0.tar.gz
```

```bash
# Create the runner and start the configuration experience
$ ./config.sh --url https://github.com/FrancoChSa28/quality-measurement --token AQ4UWRU5NVJT7COKA2QDQ5LJTXQCY

# Last step, run it!
$ ./run.sh
```