#!/usr/bin/env python3
"""
Whisper transcription script
Called from Java to transcribe audio files
"""
import sys
import io
if sys.stdout.encoding != "utf-8": sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
if sys.stdin.encoding != "utf-8": sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8")
if not hasattr(sys, "get_int_max_str_digits"):
    def g(): return 4300
    def s(maxdigits): pass
    sys.get_int_max_str_digits = g
    sys.set_int_max_str_digits = s
import json
import argparse
from faster_whisper import WhisperModel
import torch

# Force UTF-8 encoding for stdout
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def get_device():
    """Detect best available device: cuda -> mps -> cpu"""
    if torch.cuda.is_available():
        return "cuda"
    # Note: faster-whisper doesn't support MPS yet (only CUDA/CPU), 
    # but we'll prepare the logic for other scripts.
    return "cpu"

def transcribe(audio_file, model_size="large", language=None, align=False):
    """
    Transcribe audio file using Whisper

    Args:
        audio_file: Path to audio file
        model_size: Whisper model size (tiny, base, small, medium, large)
        language: Optional language code
        align: Whether to use WhisperX alignment (requires whisperx package)

    Returns:
        List of segments with text and timestamps
    """
    try:
        # Load model
        device = get_device()
        print(f"Using device: {device}", file=sys.stderr)
        model = WhisperModel(model_size, device=device, compute_type="int8")

        # Transcribe
        segments, info = model.transcribe(
            audio_file,
            language=language,
            beam_size=5,
            word_timestamps=True
        )

        # Convert to list
        results = []
        for segment in segments:
            results.append({
                "start": segment.start,
                "end": segment.end,
                "text": segment.text.strip()
            })

        # If alignment is requested and whisperx is available, use it
        if align:
            try:
                import whisperx
                import torch

                # Load audio
                audio = whisperx.load_audio(audio_file)

                # Align whisper output
                model_a, metadata = whisperx.load_align_model(
                    language_code=info.language,
                    device="cpu"
                )

                # Convert results to whisperx format
                whisper_result = {
                    "segments": results,
                    "language": info.language
                }

                # Perform alignment
                aligned_result = whisperx.align(
                    whisper_result["segments"],
                    model_a,
                    metadata,
                    audio,
                    "cpu"
                )

                # Update results with aligned timestamps
                if "segments" in aligned_result:
                    results = []
                    for segment in aligned_result["segments"]:
                        results.append({
                            "start": segment.get("start", 0),
                            "end": segment.get("end", 0),
                            "text": segment.get("text", "").strip()
                        })

            except ImportError:
                # WhisperX not available, use regular timestamps
                pass
            except Exception as align_error:
                # Alignment failed, use regular timestamps
                print(f"Warning: Alignment failed: {align_error}", file=sys.stderr)

        return {
            "language": info.language,
            "segments": results
        }

    except Exception as e:
        return {
            "error": str(e)
        }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("audio_file", help="Path to audio file")
    parser.add_argument("--model", default="large", help="Whisper model size")
    parser.add_argument("--language", default=None, help="Language code (optional)")
    parser.add_argument("--align", action="store_true", help="Use WhisperX alignment for better timing")

    args = parser.parse_args()

    result = transcribe(args.audio_file, args.model, args.language, args.align)
    print(json.dumps(result, ensure_ascii=False))
