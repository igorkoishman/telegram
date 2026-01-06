#!/usr/bin/env python3
"""
NLLB translation script
Uses facebook/nllb-200-distilled-600M model
"""
import argparse
import json
import sys

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
        from transformers import pipeline
        import warnings
        # Suppress the cache_dir warning
        warnings.filterwarnings("ignore", message=".*model_kwargs.*")
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

    print(f"Loading NLLB model for {src_code} -> {tgt_code}...", file=sys.stderr)

    # Create translation pipeline
    # Note: cache_dir causes issues with some transformers versions, so we use model_kwargs instead
    from transformers import AutoModelForSeq2SeqLM, AutoTokenizer

    model = AutoModelForSeq2SeqLM.from_pretrained(
        "facebook/nllb-200-distilled-600M",
        cache_dir=model_cache_dir
    )
    tokenizer = AutoTokenizer.from_pretrained(
        "facebook/nllb-200-distilled-600M",
        cache_dir=model_cache_dir
    )

    translator = pipeline(
        "translation",
        model=model,
        tokenizer=tokenizer,
        src_lang=src_code,
        tgt_lang=tgt_code
    )

    print(f"Translating text...", file=sys.stderr)
    try:
        result = translator(text, max_length=512)
        return result[0]["translation_text"]
    except Exception as e:
        print(f"Translation error: {e}", file=sys.stderr)
        raise


def main():
    parser = argparse.ArgumentParser(description="Translate text using NLLB")
    parser.add_argument("--text", required=True, help="Text to translate")
    parser.add_argument("--src-lang", required=True, help="Source language code")
    parser.add_argument("--tgt-lang", required=True, help="Target language code")
    parser.add_argument("--model-cache-dir", default="./models", help="Model cache directory")

    args = parser.parse_args()

    try:
        translated = translate_text(
            args.text,
            args.src_lang,
            args.tgt_lang,
            args.model_cache_dir
        )

        # Output JSON result
        result = {
            "original": args.text,
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
