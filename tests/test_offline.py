import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import zepp_auto as z

def test_classify():
    before = "本站提交成功率很高"
    assert z.classify(before, before + "\n同步成功") == "ACCEPTED"
    assert z.classify(before, before + "\n提交失败") == "FAILED"
    assert z.classify(before, before + "\n输入验证码") == "MANUAL"
    assert z.classify(before, before) == "UNKNOWN"
    assert z.classify(before, before + "\n提交失败\n同步成功") == "FAILED"

def test_valid_target():
    assert z.valid_target("https://zepp.net.cn/")
    assert not z.valid_target("https://zepp.net.cn.attacker.com/")
    assert not z.valid_target("http://zepp.net.cn/")

def test_validate_steps():
    assert z.validate_steps(15570) == 15570
    for x in (-1, 0, 100000, "123"):
        try:
            z.validate_steps(x)
            assert False, "should fail"
        except ValueError:
            pass
