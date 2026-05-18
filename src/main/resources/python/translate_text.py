#!/usr/bin/env python3
"""
Translation script
Called from Java to translate text using M2M100 or NLLB
"""
import sys
import json
import argparse
import torch

# Force UTF-8 encoding for stdout
if sys.stdout.encoding != 'utf-8':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
from transformers import M2M100ForConditionalGeneration, M2M100Tokenizer

def get_device():
    """Detect best available device: cuda -> cpu"""
    if torch.cuda.is_available():
        return "cuda"
    # Note: Translation models are freezing on MPS due to embedding ops.
    # We must fallback to CPU to prevent translation hanging at 53%.
    return "cpu"

# Language code mapping
LANG_CODES = {
    "en": "en",
    "es": "es",
    "fr": "fr",
    "de": "de",
    "it": "it",
    "ru": "ru",
    "he": "he",
    "ar": "ar"
}

def translate(text, source_lang, target_lang, model_name="facebook/m2m100_418M"):
    """
    Translate text using M2M100

    Args:
        text: Text to translate
        source_lang: Source language code
        target_lang: Target language code
        model_name: HuggingFace model name

    Returns:
        Translated text
    """
    try:
        device = get_device()
        print(f"Using device: {device}", file=sys.stderr)

        # Load model and tokenizer
        model = M2M100ForConditionalGeneration.from_pretrained(model_name).to(device)
        tokenizer = M2M100Tokenizer.from_pretrained(model_name)

        # Set source language
        tokenizer.src_lang = source_lang

        # Tokenize
        encoded = tokenizer(text, return_tensors="pt", max_length=512, truncation=True).to(device)

        # Generate translation
        generated_tokens = model.generate(
            **encoded,
            forced_bos_token_id=tokenizer.get_lang_id(target_lang),
            max_length=512
        )

        # Decode
        translated_text = tokenizer.batch_decode(generated_tokens, skip_special_tokens=True)[0]

        return {
            "translated_text": translated_text
        }

    except Exception as e:
        return {
            "error": str(e)
        }

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("text", help="Text to translate")
    parser.add_argument("--source", required=True, help="Source language code")
    parser.add_argument("--target", required=True, help="Target language code")
    parser.add_argument("--model", default="facebook/m2m100_418M", help="Model name")

    args = parser.parse_args()

    result = translate(args.text, args.source, args.target, args.model)
    print(json.dumps(result, ensure_ascii=False))
