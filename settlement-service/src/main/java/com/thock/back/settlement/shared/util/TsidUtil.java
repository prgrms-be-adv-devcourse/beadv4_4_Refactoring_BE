package com.thock.back.settlement.shared.util;
import com.github.f4b6a3.tsid.TsidCreator;

public class TsidUtil {
    public static Long nextId() {
        return TsidCreator.getTsid().toLong();
    }
}