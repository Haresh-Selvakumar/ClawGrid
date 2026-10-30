"""Compare visible transcript IN MEMORY; emit/store only non-transcript match metadata."""
import argparse
import json
import re
from datetime import datetime, timezone
from pathlib import Path
from phone_ui_memory import read_ui

ROOT=Path(__file__).resolve().parents[1]
ADB=Path.home()/'AppData/Local/Android/Sdk/platform-tools/adb.exe'
PHRASES={'light on','light off','turn on the light','turn off the light','turn on bedroom light','turn off bedroom light'}
REASONS={'ACCEPTED','SILENCE','UNSUPPORTED_WORDS','LOW_CONFIDENCE','INVALID_RESULT','ROOM_REQUIRED'}

def compare(exact, normalized, expected):
    # Match Kotlin's existing ASCII whitespace collapse, without aliases or punctuation stripping.
    normalize=lambda s:re.sub(r'[ \t\n\x0b\f\r]+',' ',s.strip().lower())
    from_raw=' '.join(normalize(s) for s in exact if normalize(s))
    actual_words=from_raw.split();expected_words=expected.split()
    return dict(final_segment_count=len(exact),final_word_count=len(actual_words),
        raw_matches_prompt_after_case_whitespace_only=from_raw==expected,
        normalized_matches_prompt=normalized==expected,
        normalized_matches_raw_transformation=normalized==from_raw,
        normalized_is_allowed_phrase=normalized in PHRASES,
        mismatch_positions=[i+1 for i in range(max(len(actual_words),len(expected_words)))
            if i>=len(actual_words) or i>=len(expected_words) or actual_words[i]!=expected_words[i]])

def main(args):
    root=read_ui(ADB);exact=[];normalized=None;reason=None;vocabulary={};displayed=set();capture=None;request=None;target=None
    own_nodes=0
    for node in root.iter('node'):
        if node.get('package')!='com.aistudio.clawgrid.v2edge' or node.get('class')=='android.widget.EditText':continue
        own_nodes+=1;text=node.get('text','')
        if text.startswith('Recognized on phone: Bedroom / light / '):
            match=re.match(r'Recognized on phone: Bedroom / light / (ON|OFF)\.',text)
            if match:target=dict(room='bedroom',device='light',action=match.group(1))
        match=re.search(r'Requested: Light (ON|OFF) · ([A-Z_]+) · request #(\d+)',text)
        if match:request=dict(action=match.group(1),phase=match.group(2),id=int(match.group(3)))
        match=re.fullmatch(r'Exact final words \[\d+\]: «(.*)»',text,re.S)
        if match:exact.append(match.group(1))
        match=re.fullmatch(r'Normalized parser text: «(.*)»',text,re.S)
        if match:normalized=match.group(1)
        if text.startswith('Parser result: '):
            value=text.removeprefix('Parser result: ')
            if value in REASONS:reason=value
        if text.startswith('Model vocabulary checked: '):
            for word,identifier in re.findall(r'([a-z]+)=(\d+)',text):
                if word in {'bedroom','light','off','on','the','turn'}:vocabulary[word]=int(identifier)
        if text.startswith('• ') and text[2:] in PHRASES:displayed.add(text[2:])
        if text.startswith('Last recognition outcome: '):
            match=re.search(r'Capture (\d+) · boot (\d+) · ([A-Z_]+) · (\d+) words · minimum word confidence ([0-9.]+)',text)
            if match and match.group(3) in REASONS:
                capture=dict(capture=int(match.group(1)),boot=int(match.group(2)),reason=match.group(3),
                             word_count=int(match.group(4)),displayed_minimum_confidence=float(match.group(5)))
    result=dict(source='PHONE_UI_MEMORY_ONLY_COMPARISON',utc=datetime.now(timezone.utc).isoformat(),
        own_app_nodes=own_nodes,preview_visible=normalized is not None and bool(exact),parser_reason=reason,
        displayed_phrase_count=len(displayed),model_vocabulary=vocabulary,metadata=capture,
        typed_recognized_target=target,displayed_request=request,
        transcript_saved=False,ui_dump_file_created=False,
        limits='No exact/normalized transcript emitted or persisted. Known prompt compared locally; no recognition-accuracy claim.')
    if result['preview_visible']:result['comparison']=compare(exact,normalized,args.expected)
    if args.output:
        output=Path(args.output).resolve()
        if not output.is_relative_to((ROOT/'benchmarks/review').resolve()) or output.exists():
            raise RuntimeError('Metadata destination outside review or already exists')
        output.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps(result,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--expected',choices=sorted(PHRASES),default='light off')
    p.add_argument('--output',help='Only derived metadata is stored, never a transcript')
    main(p.parse_args())
