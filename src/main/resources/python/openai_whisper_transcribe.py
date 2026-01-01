#!/usr/bin/env python3
"""
OpenAI Whisper transcription with optional WhisperX alignment
"""
import argparse
import json
import sys
import os

def transcribe(audio_path, model_size="large", language=None, align_output=True):
    """
    Transcribe audio using OpenAI Whisper with optional alignment
    """
    try:
        import whisper
        import whisperx
        import torch
    except ImportError as e:
        print(f"Error: Required library not found: {e}", file=sys.stderr)
        print("Install with: pip install openai-whisper whisperx", file=sys.stderr)
        sys.exit(1)

    # Determine device
    device = "cuda" if torch.cuda.is_available() else "cpu"
    print(f"Using device: {device}", file=sys.stderr)

    # Load OpenAI Whisper model
    print(f"Loading OpenAI Whisper model: {model_size}", file=sys.stderr)
    model = whisper.load_model(model_size, device=device)

    # Transcribe
    print(f"Transcribing: {audio_path}", file=sys.stderr)
    result = model.transcribe(audio_path, language=language)
    detected_lang = result.get("language", language)
    print(f"Detected language: {detected_lang}", file=sys.stderr)

    segments = result["segments"]

    # Apply WhisperX alignment if requested
    if align_output:
        try:
            print("Applying WhisperX alignment...", file=sys.stderr)
            model_a, metadata = whisperx.load_align_model(
                language_code=detected_lang or "und",
                device=device
            )
            aligned_result = whisperx.align(
                segments,
                model_a,
                metadata,
                audio_path,
                device
            )
            segments = aligned_result["segments"]
            print("Alignment completed successfully", file=sys.stderr)
        except Exception as e:
            print(f"Warning: Alignment failed: {e}", file=sys.stderr)
            print("Continuing with unaligned segments", file=sys.stderr)

    # Format output as SRT-style segments
    output = []
    for seg in segments:
        output.append({
            "start": seg["start"],
            "end": seg["end"],
            "text": seg["text"].strip()
        })

    return {
        "segments": output,
        "language": detected_lang
    }


def main():
    parser = argparse.ArgumentParser(description="Transcribe audio using OpenAI Whisper")
    parser.add_argument("audio_file", help="Path to audio file")
    parser.add_argument("--model", default="large", help="Model size (tiny, base, small, medium, large)")
    parser.add_argument("--language", default=None, help="Language code (optional, for auto-detect leave empty)")
    parser.add_argument("--align", action="store_true", help="Enable WhisperX alignment")

    args = parser.parse_args()

    if not os.path.exists(args.audio_file):
        print(f"Error: Audio file not found: {args.audio_file}", file=sys.stderr)
        sys.exit(1)

    result = transcribe(
        args.audio_file,
        model_size=args.model,
        language=args.language,
        align_output=args.align
    )

    # Output JSON result
    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
