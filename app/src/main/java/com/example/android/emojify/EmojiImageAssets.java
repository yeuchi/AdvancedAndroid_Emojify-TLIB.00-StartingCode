package com.example.android.emojify;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctyeung on 1/6/18.
 */

public class EmojiImageAssets
{
    private static final List<Integer> heads = new ArrayList<Integer>() {{
        add(R.drawable.closed_frown);
        add(R.drawable.closed_smile);
        add(R.drawable.leftwinkfrown);
        add(R.drawable.leftwink);
        add(R.drawable.rightwinkfrown);
        add(R.drawable.rightwink);
        add(R.drawable.frown);
        add(R.drawable.smile);
    }};

    public static List<Integer> getHeads() {
        return heads;
    }
}
