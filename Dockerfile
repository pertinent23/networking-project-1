FROM openjdk:17-jdk-slim
WORKDIR .

# On copie TOUS les fichiers .java
COPY *.java .

# On compile TOUS les fichiers .java
RUN javac *.java

# La commande par défaut sera de lancer le serveur
CMD ["java", "TictactoeClient"]