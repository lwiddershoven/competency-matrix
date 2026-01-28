#!/bin/bash
# Migration script to split competencies.yaml into multiple files
# T003: Create migration script to split competencies.yaml into category and role files

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SEED_DIR="$PROJECT_ROOT/src/main/resources/seed"
CATEGORIES_DIR="$SEED_DIR/categories"
ROLES_DIR="$SEED_DIR/roles"
SOURCE_FILE="$SEED_DIR/competencies.yaml"

echo "=== Competencies YAML Splitter ==="
echo "Source: $SOURCE_FILE"
echo "Categories output: $CATEGORIES_DIR"
echo "Roles output: $ROLES_DIR"
echo ""

# Check if source file exists
if [ ! -f "$SOURCE_FILE" ]; then
    echo "ERROR: Source file not found: $SOURCE_FILE"
    exit 1
fi

# Create output directories
mkdir -p "$CATEGORIES_DIR"
mkdir -p "$ROLES_DIR"

# Function to convert display name to filename
to_filename() {
    echo "$1" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9 ]//g' | tr ' ' '-' | sed 's/-\+/-/g' | sed 's/^-//;s/-$//'
}

# Use Python to parse and split the YAML file
python3 <<'PYTHON_SCRIPT'
import yaml
import sys
import os
import re

def to_filename(name):
    """Convert display name to filename"""
    filename = name.lower()
    filename = re.sub(r'[^a-z0-9 ]', '', filename)
    filename = filename.replace(' ', '-')
    filename = re.sub(r'-+', '-', filename)
    filename = filename.strip('-')
    return filename + '.yaml'

# Load source YAML
source_file = os.environ['SOURCE_FILE']
categories_dir = os.environ['CATEGORIES_DIR']
roles_dir = os.environ['ROLES_DIR']

print(f"Reading {source_file}...")

with open(source_file, 'r', encoding='utf-8') as f:
    data = yaml.safe_load(f)

# Split categories
if 'categories' in data:
    categories = data['categories']
    print(f"Found {len(categories)} categories")

    for category in categories:
        name = category.get('name', 'unknown')
        filename = to_filename(name)
        output_path = os.path.join(categories_dir, filename)

        print(f"  Writing {name} -> {filename}")

        with open(output_path, 'w', encoding='utf-8') as f:
            yaml.dump(category, f, allow_unicode=True, default_flow_style=False, sort_keys=False)

# Split roles
if 'roles' in data:
    roles = data['roles']
    print(f"Found {len(roles)} roles")

    for role in roles:
        name = role.get('name', 'unknown')
        filename = to_filename(name)
        output_path = os.path.join(roles_dir, filename)

        print(f"  Writing {name} -> {filename}")

        with open(output_path, 'w', encoding='utf-8') as f:
            yaml.dump(role, f, allow_unicode=True, default_flow_style=False, sort_keys=False)

# Write progressions to separate file
if 'progressions' in data:
    progressions = data['progressions']
    print(f"Found {len(progressions)} progressions")

    progressions_file = os.path.join(os.path.dirname(source_file), 'progressions.yaml')
    print(f"  Writing progressions -> progressions.yaml")

    with open(progressions_file, 'w', encoding='utf-8') as f:
        # Write as a simple list, not wrapped in 'progressions:' key
        yaml.dump(progressions, f, allow_unicode=True, default_flow_style=False, sort_keys=False)

print("\n✓ Split completed successfully")
PYTHON_SCRIPT

echo ""
echo "=== Verification ==="
echo "Categories created: $(ls -1 $CATEGORIES_DIR/*.yaml 2>/dev/null | wc -l)"
echo "Roles created: $(ls -1 $ROLES_DIR/*.yaml 2>/dev/null | wc -l)"
echo "Progressions file: $([ -f $SEED_DIR/progressions.yaml ] && echo 'exists' || echo 'missing')"
echo ""
echo "✓ Migration complete"
