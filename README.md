# Compile
javac -cp "lib\*" -d build -sourcepath src src\com\checkmates\main\Login.java

# Run
java -cp "build;lib\*" com.checkmates.main.Login

# Clean rebuild
Remove-Item -Path "build\*" -Recurse -Force; javac -cp "lib\*" -d build -sourcepath src src\com\checkmates\main\Login.java; java -cp "build;lib\*" com.checkmates.main.Login
