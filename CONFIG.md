# SonarQube + Github Action

## Archivos importantes

`workflow.yml`
```yaml
name: SonarQube Analysis
on:
  push:
    branches: [master, develop]
    pull_request:
      types: [opened, synchronize, reopened]

permissions:
  contents: read

jobs:
  sonarqube:
    runs-on: self-hosted
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      - name: SonarQube Scan
        run: |
          mvn clean verify sonar:sonar \
            -Dsonar.projectKey=quality-measurement \
            -Dsonar.host.url=http://localhost:9000 \
            -Dsonar.login=sqp_c146996a60ad6a698cee8236f917f0c193668288
      
```

`docker-compose.yml`
```yml
services:
  sonarqube-db:
    image: postgres:15-alpine
    container_name: sonarqube-db
    restart: unless-stopped
    environment:
      POSTGRES_USER: sonar
      POSTGRES_PASSWORD: sonar_password    
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

  sonarqube:
    image: sonarqube:lts-community
    container_name: sonarqube
    depends_on:
      sonarqube-db:
        condition: service_healthy
    environment:
      # Conexión a la base de datos
      SONAR_JDBC_URL: jdbc:postgresql://sonarqube-db:5432/sonarqube
      SONAR_JDBC_USERNAME: sonar
      SONAR_JDBC_PASSWORD: sonar_password   # Debe coincidir con POSTGRES_PASSWORD

      # Configuración de memoria (ajustar según recursos del servidor)
      SONAR_CE_JAVAOPTS: "-Xmx512m -Xms512m"
      SONAR_WEB_JAVAOPTS: "-Xmx512m -Xms512m"
      SONAR_SEARCH_JAVAOPTS: "-Xmx512m -Xms512m"

    ports:
      - "9000:9000"     # UI y API de SonarQube
    volumes:
      - sonarqube_data:/opt/sonarqube/data
      - sonarqube_extensions:/opt/sonarqube/extensions
      - sonarqube_logs:/opt/sonarqube/logs
    networks:
      - sonarqube-net
    
volumes:
  sonarqube_db_data:
    driver: local
  sonarqube_data:
    driver: local
  sonarqube_extensions:
    driver: local
  sonarqube_logs:
    driver: local

networks:
  sonarqube-net:
    driver: bridge
```

## Configuración del self-runner

Siguiendo los pasos de la [documentación oficial](https://docs.github.com/en/actions/hosting-your-own-runners/managing-self-hosted-runners/adding-self-hosted-runners).

- En tu repositorio de GitHub: **Settings → Actions → Runners → New self-hosted runner**
- GitHub te dará los comandos para instalar el agente. En mi caso, fueron los siguientes:

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

## Ejecución

### Levantar docker

**Pre-requisito**: Se debe configurar el `vm.max_map_count` en los sistemas Unix.

```bash	
# Elasticsearch (embebido en SonarQube) requiere este parámetro
sudo sysctl -w vm.max_map_count=262144

# Para hacerlo permanente:
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
```	

Para levantar el SonarQube y su base de datos:
```bash
cd .build
docker-compose -f docker-compose.yml up -d
```

### Análisis en local manualmente
```bash
mvn clean verify sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=your_sonar_token
```

### Análisis automático
