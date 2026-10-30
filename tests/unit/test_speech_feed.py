"""Host tests of the actual portable PCM framing/window/reference boundaries."""
import subprocess
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]


def test_configured_window_and_reference_isolation(tmp_path):
    source=tmp_path/'speech.cpp'; exe=tmp_path/'speech.exe'
    source.write_text(r'''
#include "speech_feed.h"
#include "pcm16.h"
#include <assert.h>
int main() {
  // Previous phone clips ended at97/157 frames, before the observed198-frame timeout.
  assert(speech_feed_frames(33280,512,SpeechInput::ReferenceLegacy)==97);
  assert(speech_feed_frames(64000,512,SpeechInput::ReferenceLegacy)==157);
  assert(speech_feed_frames(33280,512,SpeechInput::Application)==601);
  assert(speech_feed_frames(64000,512,SpeechInput::Application)==601);
  assert(speech_feed_frames(44829,512,SpeechInput::ReferenceLegacy)==119);
  assert(speech_feed_frames(44829,512,SpeechInput::ReferenceWindow)==601);
  assert(speech_feed_frames(44829,512,SpeechInput::ReferenceResetFirst)==601);
  assert(speech_feed_frames(44829,512,SpeechInput::ReferenceFreshWindow)==601);
  assert(speech_feed_frames(44829,512,SpeechInput::ReferenceOfficial)==601);
  assert(speech_feed_frames(44829,512,SpeechInput::ReferencePinned)==601);
  assert(!speech_stop_on_timeout(SpeechInput::ReferencePinned));
  assert(speech_stop_on_timeout(SpeechInput::Application));
  assert(speech_drop_partial_frame(SpeechInput::ReferencePinned,44544,512,44829));
  assert(!speech_drop_partial_frame(SpeechInput::ReferencePinned,44032,512,44829));
  assert(!speech_drop_partial_frame(SpeechInput::Application,44544,512,44829));
  assert(speech_trace_reference(SpeechInput::ReferenceDirect));
  assert(speech_trace_reference(SpeechInput::ReferenceTrace));
  assert(speech_drop_partial_frame(SpeechInput::ReferenceDirect,44544,512,44829));
  assert(!speech_stop_on_timeout(SpeechInput::ReferenceTrace));
  assert(speech_stop_on_timeout(SpeechInput::ReferenceSynthetic));
  assert(speech_stop_on_timeout(SpeechInput::ReferenceGain15));
  assert(speech_is_reference(SpeechInput::ReferenceAfe));
  assert(speech_afe_feed_frames(44829)==160);
  assert(speech_afe_feed_frames(44828)==0);
  assert((speech_afe_feed_frames(44829)-8)*512>8*512+44829);
  assert(speech_afe_feed_frames(44829,160)==511);
  assert(speech_afe_feed_frames(44829,0)==0);
  assert(speech_afe_feed_frames(32000,160)==431);
  assert((speech_afe_feed_frames(32000,160)*160/512-8)*512>4096+32000);
  assert(speech_is_reference(SpeechInput::ReferencePhoneAfe));
  assert(speech_reference_bytes(SpeechInput::ReferencePhoneAfe)==64000);
  assert(speech_reference_matches(64000,679739021u,SpeechInput::ReferencePhoneAfe));
  assert(!speech_reference_matches(64000,679739020u,SpeechInput::ReferencePhoneAfe));
  assert(!speech_reference_matches(64000,679739021u,SpeechInput::ReferencePinned));
  assert(speech_phone_reference(SpeechInput::ReferencePhoneBedroom));
  assert(speech_reference_matches(64000,679739021u,SpeechInput::ReferencePhoneBedroom));
  assert(speech_decoder_trace(SpeechInput::ReferencePhoneTrace));
  assert(!speech_decoder_trace(SpeechInput::Application));
  assert(speech_reference_matches(69760,2182999710u,SpeechInput::ReferenceZiraTrace));
  assert(!speech_reference_matches(69760,2182999711u,SpeechInput::ReferenceZiraTrace));
  assert(speech_reference_matches(64000,2109903275u,SpeechInput::ReferenceSilenceTrace));
  assert(speech_reference_matches(67520,3492560341u,SpeechInput::ReferenceNonCommandTrace));
  assert(!speech_reference_matches(64000,679739021u,SpeechInput::ReferenceSilenceTrace));
  assert(speech_reference_matches(64000,679739021u,SpeechInput::ReferencePhoneSplit));
  assert(speech_reference_matches(69760,2182999710u,SpeechInput::ReferenceZiraSplit));
  assert(speech_reference_matches(64000,2109903275u,SpeechInput::ReferenceSilenceSplit));
  assert(!speech_reference_matches(64000,679739021u,SpeechInput::ReferenceNonCommandSplit));
  assert((speech_afe_feed_frames(44829,160)*160/512-8)*512>4096+44829);
  assert(speech_feed_frames(64001,512,SpeechInput::Application)==0);
  assert(speech_feed_frames(1600,0,SpeechInput::Application)==0);
  const uint8_t raw[]={0,128,255,127,0,0,255,255,1,0};
  int16_t frame[4]; assert(pcm_frame(raw,sizeof(raw),3,frame,4)==2);
  assert(frame[0]==-1 && frame[1]==1 && frame[2]==0 && frame[3]==0);
  assert(pcm_frame_delayed(raw,sizeof(raw),1,3,frame,4)==2);
  assert(frame[0]==0 && frame[1]==0 && frame[2]==-32768 && frame[3]==32767);
  assert(pcm_frame_delayed(raw,sizeof(raw),0,10,frame,4)==0);
  assert(frame[0]==0 && frame[3]==0);
  auto levels=pcm_levels(raw,sizeof(raw));
  assert(levels.peak==32768 && levels.clipped==2 && levels.nonzero==4 && levels.sum==-1);
  const uint8_t check[]="123456789"; assert(pcm_crc32(check,9)==0xcbf43926u);
  assert(kSpeechReferenceId>16);
  assert(speech_reference_matches(89658,330285049));
  assert(!speech_reference_matches(89656,330285049));
  assert(!speech_reference_matches(89658,330285048));
  assert(speech_reference_matches(89658,2905759910u,SpeechInput::ReferenceSynthetic));
  assert(!speech_reference_matches(89658,330285049,SpeechInput::ReferenceSynthetic));
  assert(!speech_reference_matches(89658,2905759910u,SpeechInput::ReferencePinned));
}
''',encoding='utf-8')
    subprocess.run([sys.executable,'-m','ziglang','c++','-std=c++17','-O2','-I'+str(ROOT/'firmware/common/runtime'),str(source),'-o',str(exe)],check=True,capture_output=True,timeout=120)
    subprocess.run([str(exe)],check=True,capture_output=True,timeout=5)
