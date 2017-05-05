package com.ljstudio.android.loveday.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.ljstudio.android.loveday.MyApplication;
import com.ljstudio.android.loveday.R;
import com.ljstudio.android.loveday.adapter.DaysAdapter;
import com.ljstudio.android.loveday.adapter.QuickDaysAdapter;
import com.ljstudio.android.loveday.constants.Constant;
import com.ljstudio.android.loveday.entity.DaysData;
import com.ljstudio.android.loveday.eventbus.MessageEvent;
import com.ljstudio.android.loveday.utils.DateFormatUtil;
import com.ljstudio.android.loveday.utils.DateUtil;
import com.ljstudio.android.loveday.utils.PreferencesUtil;
import com.ljstudio.android.loveday.utils.SystemOutUtil;
import com.ljstudio.android.loveday.utils.ToastUtil;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.ljstudio.android.loveday.greendao.DaysDataDao;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.id_main_toolbar)
    Toolbar toolbar;
    @BindView(R.id.id_main_top_title)
    TextView tvTopTitle;
    @BindView(R.id.id_main_top_date)
    TextView tvTopDate;
    @BindView(R.id.id_main_top_days)
    TextView tvTopDays;
    @BindView(R.id.id_main_top_unit)
    TextView tvTopUnit;
    @BindView(R.id.id_main_recycler_view)
    RecyclerView recyclerView;

    private DaysAdapter daysAdapter;
    private QuickDaysAdapter quickDaysAdapter;
    private List<DaysData> listDays = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        EventBus.getDefault().register(this);

        toolbar.setTitle(R.string.app_name);
        toolbar.setTitleTextColor(getResources().getColor(R.color.colorWhite));
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(onMenuItemClick);

        setStatusBar(ContextCompat.getColor(this, R.color.colorPrimary));

        if(checkIfFirst()) {
            testData();
        } else {
            resetData();
        }
    }

    private void initListData() {
        LinearLayoutManager layoutManager1 = new LinearLayoutManager(MainActivity.this);
        layoutManager1.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager1);
//        recyclerView.addItemDecoration(new RecyclerViewDividerItem(this, VERTICAL_LIST, R.color.colorGrayLight));
        recyclerView.addItemDecoration(new HorizontalDividerItemDecoration.Builder(this).build());

//        daysAdapter = new DaysAdapter(MainActivity.this, listDays);
//        recyclerView.setAdapter(daysAdapter);

        quickDaysAdapter = new QuickDaysAdapter(R.layout.layout_item_days, listDays);
        recyclerView.setAdapter(quickDaysAdapter);

        quickDaysAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra(DetailActivity.ID, listDays.get(position).getId());
                startActivity(intent);
            }
        });
    }

    private void initTopData() {
        DaysData data = listDays.get(0);

        tvTopTitle.setText(data.getTitle());
        tvTopDate.setText("起始日：" + data.getDate());

        Date date = DateFormatUtil.convertStr2Date(data.getDate(), DateFormatUtil.sdfDate1);
        int days = DateUtil.betweenDays(date, new Date());
        tvTopDays.setText(String.valueOf(days));

        tvTopUnit.setText(data.getUnit());
    }

    private void writeOne2DB(final DaysData data) {
        try {
            MyApplication.getDaoSession(this).runInTx(new Runnable() {
                @Override
                public void run() {
                    MyApplication.getDaoSession(MainActivity.this).getDaysDataDao().insertOrReplace(data);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeAll2DB(final List<DaysData> data) {
        try {
            MyApplication.getDaoSession(this).runInTx(new Runnable() {
                @Override
                public void run() {
                    int size = 0;
                    for (DaysData entity : data) {
                        MyApplication.getDaoSession(MainActivity.this).getDaysDataDao().insertOrReplace(entity);
                        size = size + 1;

                        if (size >= data.size()) {
                            ToastUtil.showToast(MainActivity.this, "数据存储成功");
                        }
                    }

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readOne4DB(Long id) {
        final DaysDataDao dao = MyApplication.getDaoSession(this).getDaysDataDao();
        List<DaysData> list = dao.queryBuilder()
                .where(DaysDataDao.Properties.Id.eq(id))
                .orderAsc(DaysDataDao.Properties.Id)
                .build().list();
    }

    private List<DaysData> readAll4DB() {
        final DaysDataDao dao = MyApplication.getDaoSession(this).getDaysDataDao();
        List<DaysData> list = dao.queryBuilder()
                .orderAsc(DaysDataDao.Properties.Id)
                .build().list();

        return list;
    }

    private void testData() {
        DaysData data1 = new DaysData();
        data1.setTitle("ttt.XY一起美丽时光");
        data1.setDate("2015-11-26");
        data1.setDays("1");
        data1.setUnit("天");
        data1.setIsTop(true);
        writeOne2DB(data1);

        setIsInstall();

        resetData();
    }

    private void resetData() {
        listDays.clear();
        listDays = readAll4DB();
        SystemOutUtil.sysOut("listDays.size()-->" + listDays.size());

        initTopData();
        initListData();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MainActivity.this.getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    private Toolbar.OnMenuItemClickListener onMenuItemClick = new Toolbar.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.id_action_add) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, EditActivity.class);
                startActivity(intent);
            } else if (id == R.id.id_action_output) {
                ToastUtil.showToast(MainActivity.this, "备份成功");
            }

            return true;
        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (200 == event.message) {
            resetData();
        }
    }

    private boolean checkIfFirst() {
        Constant.bIsFirst = PreferencesUtil.getPrefBoolean(this, Constant.IS_FIRST, true);
        return Constant.bIsFirst;
    }

    private void setIsInstall() {
        PreferencesUtil.setPrefBoolean(this, Constant.IS_FIRST, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void setStatusBar(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            tintManager.setNavigationBarTintEnabled(false);
            tintManager.setTintColor(color);
        }
    }

}