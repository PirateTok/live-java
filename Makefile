OUT := out

.PHONY: build test clean discipline

# lib + examples (examples are a Maven source root); Maven supplies jackson on the classpath
build:
	mvn -q compile
	@echo "build ok"

test:
	mvn -q test

clean:
	rm -rf $(OUT) target

discipline:
	@mkdir -p $(OUT)
	javac -d $(OUT) --release 25 discipline/Scanner.java
	java -cp $(OUT) discipline.Scanner src/main/java examples
