# Novel pack pipeline (internal)

Operator docs (Chinese): [`docs/小说题包流水线操作手册.md`](../../docs/小说题包流水线操作手册.md)

```bash
export DEEPSEEK_API_KEY=...
export NOVEL_CORPUS_ROOT=$HOME/Documents/novels
pip install -r scripts/novel_pipeline/requirements.txt
python scripts/novel_pipeline/stages/s1_extract_chapter.py --book shuihu --chapter 23
```
