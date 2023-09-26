package ru.yandex.practicum.contacts.presentation.main;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.yandex.practicum.contacts.R;
import ru.yandex.practicum.contacts.databinding.ActivityMainBinding;
import ru.yandex.practicum.contacts.model.ContactType;
import ru.yandex.practicum.contacts.presentation.filter.FilterContactTypeDialogFragment;
import ru.yandex.practicum.contacts.presentation.main.model.MenuClick;
import ru.yandex.practicum.contacts.presentation.sort.SortDialogFragment;
import ru.yandex.practicum.contacts.presentation.sort.model.SortType;
import ru.yandex.practicum.contacts.ui.widget.DividerItemDecoration;
import ru.yandex.practicum.contacts.utils.android.Debouncer;
import ru.yandex.practicum.contacts.utils.android.OnDebounceListener;
import ru.yandex.practicum.contacts.utils.widget.EditTextUtils;

@SuppressLint("UnsafeExperimentalUsageError")
public class MainActivity extends AppCompatActivity  implements OnDebounceListener {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private ContactAdapter adapter;

    private final Map<Integer, BadgeDrawable> badges = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setTitleTextAppearance(this, R.style.Toolbar_Title);

        adapter = new ContactAdapter();
        binding.recycler.setAdapter(adapter);

        final DividerItemDecoration decoration = new DividerItemDecoration(this, R.drawable.item_decoration_72dp,
                DividerItemDecoration.VERTICAL);
        binding.recycler.addItemDecoration(decoration);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        viewModel.getContactsLiveDate().observe(this, this::updateContacts);
        binding.searchLayout.resetButton.setOnClickListener(v -> clearSearch());

        createBadges();
        bindSearch();
        EditTextUtils.addTextListener(binding.searchLayout.searchText, query -> viewModel.updateSearchText(query.toString()));

        binding.searchLayout.resetButton.setOnClickListener(view -> clearSearch());

        getSupportFragmentManager().setFragmentResultListener(SortDialogFragment.REQUEST_KEY, this, (requestKey, result) -> {
            final SortType newSortType = SortDialogFragment.from(result);
            viewModel.updateSortType(newSortType);
        });

        getSupportFragmentManager().setFragmentResultListener(FilterContactTypeDialogFragment.REQUEST_KEY, this, (requestKey, result) -> {
            final Set<ContactType> newFilterContactTypes = FilterContactTypeDialogFragment.from(result);
            viewModel.updateFilterContactTypes(newFilterContactTypes);
        });
    }

    public void bindSearch() {
        final Debouncer debouncer = new Debouncer(this);
        binding.searchLayout.searchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                debouncer.updateValue(s.toString());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        attachBadges();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_sort) {
            viewModel.onMenuClick(MenuClick.SORT);
            return true;
        }
        if (id == R.id.menu_filter) {
            viewModel.onMenuClick(MenuClick.FILTER);
            return true;
        }
        if (id == R.id.menu_search) {
            viewModel.onMenuClick(MenuClick.SEARCH);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        viewModel.onBackPressed();
    }

    private void updateContacts(List<ContactUi> contacts) {
        adapter.setItems(contacts, () -> binding.recycler.scrollToPosition(0));
        if (contacts.size() > 0) {
            binding.recycler.setVisibility(View.VISIBLE);
            binding.nothingFound.setVisibility(View.GONE);
        } else {
            binding.recycler.setVisibility(View.GONE);
            binding.nothingFound.setVisibility(View.VISIBLE);
        }
    }

    private void createBadges() {
        badges.put(R.id.menu_sort, createBadge());
        badges.put(R.id.menu_filter, createBadge());
        badges.put(R.id.menu_search, createBadge());
    }

    private void attachBadges() {
        for (Map.Entry<Integer, BadgeDrawable> entry : badges.entrySet()) {
            BadgeUtils.attachBadgeDrawable(entry.getValue(), binding.toolbar, entry.getKey());
        }
    }

    private BadgeDrawable createBadge() {
        final BadgeDrawable drawable = BadgeDrawable.create(this);
        drawable.setBackgroundColor(ContextCompat.getColor(this, R.color.color_red));
        drawable.setVisible(false);
        return drawable;
    }

    private void clearSearch() {
        binding.searchLayout.searchText.setText("");
        viewModel.search();
    }

    @Override
    public void onDebounce() {
        viewModel.search();
    }
}
