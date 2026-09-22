Comando para arrancar servidor con Docker (desde la tracking-logistico )
mvn spring-boot:run "-Dspring-boot.run.profiles=prod" 

comando estándar de Apache Maven para limpiar, compilar, probar y empaquetar un proyecto de Java
mvn clean install

Arranca el Docker e instala lo que haga falta
docker-compose up -d

Arranca el Front (desde la carpeta carpeta my-app-master)
npm run dev 

URL del PGadmin
http://localhost:5050/login?next=/browser/


PG Admin 
Name: Tracking PostgreSQL
Host: postgres
Port: 5432
Database: tracking_db
User: postgres
Password: postgres