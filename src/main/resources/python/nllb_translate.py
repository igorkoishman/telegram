#!/usr/bin/env python3
"""
NLLB translation script
Uses facebook/nllb-200-distilled-600M model
"""
import argparse
import json
import sys
import io
if sys.stdout.encoding != "utf-8": sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
if sys.stdin.encoding != "utf-8": sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding="utf-8")
if not hasattr(sys, "get_int_max_str_digits"):
    def g(): return 4300
    def s(maxdigits): pass
    sys.get_int_max_str_digits = g
    sys.set_int_max_str_digits = s
import torch

# Force UTF-8 encoding for stdout
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def get_device():
    """Detect best available device: cuda -> cpu"""
    if torch.cuda.is_available():
        return "cuda"
    # Note: Translation models are freezing on MPS due to embedding ops.
    # We must fallback to CPU to prevent translation hanging at 53%.
    return "cpu"

# NLLB language code mapping
LANG_CODE_MAP = {
    "en": "eng_Latn",
    "fr": "fra_Latn",
    "es": "spa_Latn",
    "de": "deu_Latn",
    "it": "ita_Latn",
    "ru": "rus_Cyrl",
    "he": "heb_Hebr",
    "ar": "arb_Arab",
    "iw": "heb_Hebr"
}

def translate_text(text, src_lang, tgt_lang, model_cache_dir="./models"):
    """
    Translate text using NLLB model
    """
    try:
        from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
    except ImportError:
        print("Error: transformers library not found", file=sys.stderr)
        print("Install with: pip install transformers", file=sys.stderr)
        sys.exit(1)

    # Map language codes
    src_key = src_lang.lower()
    tgt_key = tgt_lang.lower()

    if src_key not in LANG_CODE_MAP:
        raise ValueError(f"Unsupported source language: {src_lang}. Supported: {list(LANG_CODE_MAP.keys())}")
    if tgt_key not in LANG_CODE_MAP:
        raise ValueError(f"Unsupported target language: {tgt_lang}. Supported: {list(LANG_CODE_MAP.keys())}")

    src_code = LANG_CODE_MAP[src_key]
    tgt_code = LANG_CODE_MAP[tgt_key]

    device = get_device()
    print(f"Loading NLLB model for {src_code} -> {tgt_code} using {device}...", file=sys.stderr)

    # Load model and tokenizer
    model = AutoModelForSeq2SeqLM.from_pretrained(
        "facebook/nllb-200-distilled-600M",
        cache_dir=model_cache_dir
    ).to(device)
    
    tokenizer = AutoTokenizer.from_pretrained(
        "facebook/nllb-200-distilled-600M",
        cache_dir=model_cache_dir
    )

    print(f"Translating text...", file=sys.stderr)
    try:
        # Set source language and tokenize
        tokenizer.src_lang = src_code
        inputs = tokenizer(text, return_tensors="pt").to(device)

        # Generate translation with target language
        translated_tokens = model.generate(
            **inputs,
            forced_bos_token_id=tokenizer.convert_tokens_to_ids(tgt_code),
            max_length=512
        )

        # Decode
        return tokenizer.batch_decode(translated_tokens, skip_special_tokens=True)[0]
    except Exception as e:
        print(f"Translation error: {e}", file=sys.stderr)
        raise


def main():
    parser = argparse.ArgumentParser(description="Translate text using NLLB")
    parser.add_argument("--text", help="Text to translate (or '-' to read from stdin)")
    parser.add_argument("--src-lang", required=True, help="Source language code")
    parser.add_argument("--tgt-lang", required=True, help="Target language code")
    parser.add_argument("--model-cache-dir", default="./models", help="Model cache directory")

    args = parser.parse_args()

    text = args.text
    if not text or text == "-":
        text = sys.stdin.read().strip()

    if not text:
        print(json.dumps({"error": "No text provided for translation"}))
        sys.exit(1)

    try:
        translated = translate_text(
            text,
            args.src_lang,
            args.tgt_lang,
            args.model_cache_dir
        )

        # Output JSON result
        result = {
            "original": text,
            "translated": translated,
            "src_lang": args.src_lang,
            "tgt_lang": args.tgt_lang
        }
        print(json.dumps(result, ensure_ascii=False))

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
