package com.example.bookapp.models;

import android.util.Log;

import com.example.bookapp.zalo.Helper.Helpers;
import com.example.bookapp.zalo.HttpProvider;

import org.json.JSONObject;

import java.util.Date;

import okhttp3.FormBody;
import okhttp3.RequestBody;

public class CreateOrder {

    private class CreateOrderData {
        String AppId;
        String AppUser;
        String AppTime;
        String Amount;
        String AppTransId;
        String EmbedData;
        String Items;
        String BankCode;
        String Description;
        String Mac;

        private CreateOrderData(String amount, String bookId) throws Exception {
            long appTime = new Date().getTime();
            AppId = String.valueOf(AppInfo.APP_ID);
            AppUser = "Android_Demo";
            AppTime = String.valueOf(appTime);
            Amount = amount;
            AppTransId = Helpers.getAppTransId();

            EmbedData = String.format("{\"app_scheme\":\"myappscheme\", \"bookId\":\"%s\"}", bookId);


            Log.d("CreateOrder", "EmbedData = " + EmbedData);
            Items = "[]";
            BankCode = "zalopayapp";
            Description = "Thanh toán đơn sách #" + AppTransId;

            String inputHMac = String.format("%s|%s|%s|%s|%s|%s|%s",
                    AppId, AppTransId, AppUser, Amount, AppTime, EmbedData, Items);

            Mac = Helpers.getMac(AppInfo.MAC_KEY, inputHMac);
        }
    }

    public JSONObject createOrder(String amount, String bookId) throws Exception {
        CreateOrderData input = new CreateOrderData(amount, bookId);

        RequestBody formBody = new FormBody.Builder()
                .add("app_id", input.AppId)
                .add("app_user", input.AppUser)
                .add("app_time", input.AppTime)
                .add("amount", input.Amount)
                .add("app_trans_id", input.AppTransId)
                .add("embed_data", input.EmbedData)
                .add("item", input.Items)
                .add("bank_code", input.BankCode)
                .add("description", input.Description)
                .add("mac", input.Mac)
                // 🔥 Đây là dòng bạn đang thiếu
                .add("callback_url", "bookapp_group2_fu://paymentresult")
                .build();


        JSONObject data = HttpProvider.sendPost(AppInfo.URL_CREATE_ORDER, formBody);

        return data;
    }
}