SOURCES := $(shell find src/main/java examples -name "*.java" 2>/dev/null)
OUT := out

.PHONY: build clean discipline

build: $(SOURCES)
	@mkdir -p $(OUT)
	javac -d $(OUT) --release 25 $(SOURCES)
	@echo "build ok — $$(echo $(SOURCES) | wc -w) files"

clean:
	rm -rf $(OUT)

discipline:
	javac -d $(OUT) --release 25 discipline/Scanner.java
	java -cp $(OUT) discipline.Scanner src/main/java examples
