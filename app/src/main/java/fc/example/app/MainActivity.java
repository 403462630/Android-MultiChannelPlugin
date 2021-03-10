package fc.example.app;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import fc.multi.channel.library.ChannelReader;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ChannelReader.init(this, BuildConfig.DEBUG);
        Map map = ChannelReader.getExtInfo(this);
        ((TextView)findViewById(R.id.tv_text)).setText("channelId: " + ChannelReader.getChannelId(this) + ", extInfo: " + new Gson().toJson(map));
    }
}
