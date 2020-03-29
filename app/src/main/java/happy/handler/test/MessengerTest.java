package happy.handler.test;

import android.os.Bundle;
import android.os.IBinder;
import android.util.SparseArray;

import java.io.File;
import java.util.List;

import happy.handler.Messenger;

@Messenger("MyMessenger")
public interface MessengerTest {
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

    void methodCharSequence(StringBuilder charSequence);

    void methodBinder(IBinder binder);

    void methodParcelable(Bundle parcelable);

    void methodSerializable(File serializable);

    void methodAll(byte aByte, short aShort, int aInt, long aLong, float aFloat, double aDouble,
                   char aChar, boolean aBoolean, String aString, StringBuilder charSequence,
                   IBinder binder, Bundle parcelable, File serializable);

    void methodByteArray(byte[] byteArray);

    void methodShortArray(short[] shortArray);

    void methodIntArray(int[] intArray);

    void methodLongArray(long[] longArray);

    void methodFloatArray(float[] floatArray);

    void methodDoubleArray(double[] doubleArray);

    void methodCharArray(char[] charArray);

    void methodBooleanArray(boolean[] booleanArray);

    void methodStringArray(String[] stringArray);

    void methodCharSequenceArray(StringBuilder[] charSequenceArray);

    void methodParcelableArray(Bundle[] parcelableArray);

    void methodAllArray(byte[] byteArray, short[] shortArray, int[] intArray, long[] longArray,
                        float[] floatArray, double[] doubleArray, char[] charArray,
                        boolean[] booleanArray, String[] stringArray,
                        StringBuilder[] charSequenceArray, Bundle[] parcelableArray);

    void methodIntegerList(List<Integer> integerList);

    void methodStringList(List<String> stringList);

    void methodCharSequenceList(List<CharSequence> charSequenceList);

    void methodParcelableList(List<Bundle> parcelableList);

    void methodAllList(List<Integer> integerList, List<String> stringList,
                       List<CharSequence> charSequenceList, List<Bundle> parcelableList);

    void methodSparseParcelableArray(SparseArray<Bundle> sparseParcelableArray);
}
