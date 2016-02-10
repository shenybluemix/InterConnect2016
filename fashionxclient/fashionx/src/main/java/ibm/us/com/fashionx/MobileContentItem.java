package ibm.us.com.fashionx;

import android.graphics.drawable.BitmapDrawable;
import android.os.Parcel;
import android.os.Parcelable;

import com.ibm.caas.CAASContentItem;
import com.ibm.caas.CAASContentItemsList;

public class MobileContentItem implements Parcelable {

    public String image;
    public BitmapDrawable drawable;
    public CAASContentItemsList caasContentItemsList;

    public MobileContentItem() {;}

    private MobileContentItem(Parcel in) {
        image = in.readString();
    }

    public static final Parcelable.Creator<MobileContentItem> CREATOR = new Parcelable.Creator<MobileContentItem>() {
        public MobileContentItem createFromParcel(Parcel in) {
            return new MobileContentItem(in);
        }

        public MobileContentItem[] newArray(int size) {
            return new MobileContentItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(image);
    }

}
