#!/bin/bash

# Get the directory of the current .sh file
TARGET_DIR=$(dirname "$(realpath "$0")")

# Define the target file relative to the script directory
TARGET_FILE="$TARGET_DIR/AllCrazyScreens.java"

# Scan the directory for all .java files and extract class names
CLASS_LIST=$(find "$TARGET_DIR" -name "*.java" | sed -E 's|.*/||;s|\.java||')

# Generate the static block content
STATIC_BLOCK="        try {\n"
for CLASS in $CLASS_LIST; do
    STATIC_BLOCK+="            Class.forName(\"net.oktawia.crazyae2addons.screens.${CLASS}\");\n"
done
STATIC_BLOCK+="        } catch (ClassNotFoundException e) {"

# Use sed to replace the static block
sed -i -E "/try \{/,/\}/c\\$STATIC_BLOCK" "$TARGET_FILE"

echo "Static block in $TARGET_FILE has been replaced."
