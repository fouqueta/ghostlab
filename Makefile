SRC_DIR := src
OBJ_DIR := src
BIN_DIR := .

EXE := $(BIN_DIR)/server
SRC := $(wildcard $(SRC_DIR)/*.c)
OBJ := $(SRC:$(SRC_DIR)/%.c=$(OBJ_DIR)/%.o)

CFLAGS   := -Wall -Wextra
LDFLAGS  := -Llib
LDLIBS   := -lm -lpthread

.PHONY: all clean

all: $(EXE) java

$(EXE): $(OBJ) | $(BIN_DIR)
		$(CC) $(LDFLAGS) $^ $(LDLIBS) -o $@

$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c | $(OBJ_DIR)
		$(CC)  $(CFLAGS) -c $< -o $@

java:
		javac $(SRC_DIR)/*.java

distclean:
		@$(RM) -rv $(OBJ)
		rm -rv $(SRC_DIR)/*.class
