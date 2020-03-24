package happy.handler.test;

import java.util.List;

import happy.handler.Handler;

@Handler
public interface MyInterface<T extends Number> {
    void methodNoParam();

    void methodByte(byte aByte);

    void methodShort(short aShort);

    void methodInt(int aInt);

    void methodLong(long aLong);

    void methodFloat(float aFloat);

    void methodDouble(double aDouble);

    void methodChar(char aChar);

    void methodBoolean(boolean aBoolean);

    void methodString(String aString);

    void methodAll(byte aByte, short aShort, int aInt, long aLong, float aFloat, double aDouble, char aChar, boolean aBoolean, String aString);

    void methodArrayByte(byte[] aByte);

    void methodArrayShort(short[] aShort);

    void methodArrayInt(int[] aInt);

    void methodArrayLong(long[] aLong);

    void methodArrayFloat(float[] aFloat);

    void methodArrayDouble(double[] aDouble);

    void methodArrayChar(char[] aChar);

    void methodArrayBoolean(boolean[] aBoolean);

    void methodArrayString(String[] aString);

    void methodArrayAll(byte[] aByte, short[] aShort, int[] aInt, long[] aLong, float[] aFloat, double[] aDouble, char[] aChar, boolean[] aBoolean, String[] aString);

    void methodVarArgByte(byte... bytes);

    void methodVarArgString(String... strings);

    void methodGenerationType(List<T> list);

    void methodGenerationType2(List<? extends Number> numbers);

    void methodGenerationType3(List<? extends Number>[] numbersList);
}
