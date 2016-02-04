package ibm.us.com.fashionx;

import android.os.Parcel;
import android.os.Parcelable;

public class MobileFirstWeather implements Parcelable {

    public int      temperature;
    public int      maximum;
    public int      minimum;

    //weather icon code get from weather API
    public int      icon;
    public float      latitude;
    public float      longitude;
    public String   path;

    //Sun, Snow, Rain
    public String   phrase;

    //Phrases get directly from weather API
    public String   rawPhrase;

    private final String SUN = "Sun";
    private final String SNOW = "Snow";
    private final String RAIN = "Rain";

    public MobileFirstWeather() {
        phrase = "Dummy";
    }

    private MobileFirstWeather(Parcel in) {
        temperature = in.readInt();
        maximum = in.readInt();
        minimum = in.readInt();
        icon = in.readInt();
        path = in.readString();
        rawPhrase = in.readString();
        phrase = in.readString();
    }

    //Chris -- Jan.27
    //Mapping raw weather phrase getting from the weather API to three: RAIN/SNOW/SUN
    public void convertPhrase(){
        if (0 <= icon && icon <= 12){
            this.phrase = RAIN;
            return;
        }
        if (13 <= icon && icon <= 15){
            this.phrase = SNOW;
            return;
        }
        if (16 <= icon && icon <= 18){
            this.phrase  = RAIN;
            return;
        }
        if (19 <= icon && icon <=25){
            //actually it is foggy
            this.phrase  = RAIN;
            return;
        }

        if (26 <= icon && icon <=34){
            //actually it is foggy
            this.phrase  = SUN;
            return;
        }
        if (38 <= icon && icon <=40){
            //actually it is foggy
            this.phrase  = RAIN;
            return;
        }
        if ( icon == 42){
            //actually it is foggy
            this.phrase  = SNOW;
            return;
        }
        if (icon == 47){
            this.phrase  = RAIN;
            return;
        }
    }

    public void convertPhraseBySentiment(String text){


    }

    public static final Parcelable.Creator<MobileFirstWeather> CREATOR = new Parcelable.Creator<MobileFirstWeather>() {
        public MobileFirstWeather createFromParcel(Parcel in) {
            return new MobileFirstWeather(in);
        }

        public MobileFirstWeather[] newArray(int size) {
            return new MobileFirstWeather[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(temperature);
        dest.writeInt(maximum);
        dest.writeInt(minimum);
        dest.writeInt(icon);
        dest.writeString(path);
        dest.writeString(phrase);
        dest.writeString(rawPhrase);
        dest.writeFloat(latitude);
        dest.writeFloat(longitude);
    }

}
