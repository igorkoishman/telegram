#!/bin/bash

echo "=== Translation Service Setup Test ==="
echo ""

# Check Java
echo "1. Checking Java..."
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    echo "   ✓ Java found: $java_version"
else
    echo "   ✗ Java not found. Please install JDK 17+"
    exit 1
fi

# Check Maven
echo "2. Checking Maven..."
if [ -f "./mvnw" ]; then
    echo "   ✓ Maven wrapper found"
else
    echo "   ✗ Maven wrapper not found"
    exit 1
fi

# Check FFmpeg
echo "3. Checking FFmpeg..."
if command -v ffmpeg &> /dev/null; then
    ffmpeg_version=$(ffmpeg -version 2>&1 | head -n 1)
    echo "   ✓ FFmpeg found: $ffmpeg_version"
else
    echo "   ✗ FFmpeg not found. Please install: brew install ffmpeg"
    exit 1
fi

if command -v ffprobe &> /dev/null; then
    echo "   ✓ FFprobe found"
else
    echo "   ✗ FFprobe not found"
    exit 1
fi

# Check Python
echo "4. Checking Python..."
if command -v python3 &> /dev/null; then
    python_version=$(python3 --version 2>&1)
    echo "   ✓ Python3 found: $python_version"
else
    echo "   ✗ Python3 not found. Please install Python 3.8+"
    exit 1
fi

# Check Python scripts
echo "5. Checking Python scripts..."
if [ -f "src/main/resources/python/whisper_transcribe.py" ]; then
    echo "   ✓ whisper_transcribe.py found"
else
    echo "   ✗ whisper_transcribe.py not found"
    exit 1
fi

if [ -f "src/main/resources/python/translate_text.py" ]; then
    echo "   ✓ translate_text.py found"
else
    echo "   ✗ translate_text.py not found"
    exit 1
fi

# Check Python dependencies
echo "6. Checking Python dependencies..."
if python3 -c "import faster_whisper" 2>/dev/null; then
    echo "   ✓ faster-whisper installed"
else
    echo "   ⚠ faster-whisper not installed. Run:"
    echo "     pip install -r src/main/resources/python/requirements.txt"
fi

if python3 -c "import transformers" 2>/dev/null; then
    echo "   ✓ transformers installed"
else
    echo "   ⚠ transformers not installed. Run:"
    echo "     pip install -r src/main/resources/python/requirements.txt"
fi

# Check directories
echo "7. Checking directories..."
mkdir -p uploads outputs downloads
echo "   ✓ Created: uploads, outputs, downloads"

# Summary
echo ""
echo "=== Setup Test Complete ==="
echo ""
echo "Next steps:"
echo "1. Install Python dependencies if needed:"
echo "   pip install -r src/main/resources/python/requirements.txt"
echo ""
echo "2. Build the application:"
echo "   ./mvnw clean install"
echo ""
echo "3. Run the application:"
echo "   ./mvnw spring-boot:run"
echo ""
echo "4. Test the API:"
echo "   curl http://localhost:8080/api/translation/status/test"
echo ""
