package com.example.todoapp.ui.todolist

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.res.Resources
import android.util.TypedValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.todoapp.R
import com.example.todoapp.TodoApp
import com.example.todoapp.databinding.FragmentTodoListBinding
import com.example.todoapp.di.scope.FragmentScope
import com.example.todoapp.ui.ViewModelFactory
import com.example.todoapp.ui.todolist.actions.TodoListUiEvent
import com.example.todoapp.ui.todolist.recyclerview.PreviewOffsetTodoItemDecoration
import com.example.todoapp.ui.todolist.recyclerview.TodoItemsAdapter
import com.example.todoapp.utils.generateRandomItemId
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@FragmentScope
class TodoListFragment : Fragment() {
    private var _binding: FragmentTodoListBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val viewModel: TodoListViewModel by viewModels { viewModelFactory }

//    @Inject
//    lateinit var todoItemsAdapter: TodoItemsAdapter

    private var snackbar : Snackbar? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().application as TodoApp)
            .appComponent
            .todoListFragmentComponent()
            .inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodoListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            setupUiEventsListener()
        }
        setupRecycler()
        setupErrorHandler()
        setupPullRefresh()

        binding.floatingActionButton.setOnClickListener { navigateToNewTodoItem() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupErrorHandler() {
        viewModel.errorListLiveData().observe(viewLifecycleOwner) {
            if (it) {
                setupSnackbar(R.string.load_error)
            } else {
                dismissSnackbar()
            }
        }
        viewModel.errorItemLiveData().observe(viewLifecycleOwner) {
            if (it) {
                setupSnackbar(R.string.item_error)
            } else {
                dismissSnackbar()
            }
        }
    }

    private fun setupSnackbar(message: Int) {
        if (snackbar == null) {
            snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE)
            snackbar?.show()
        }
    }

    private fun setupPullRefresh() {
        binding.swipeToRefresh.setOnRefreshListener {
            dismissSnackbar()
            viewModel.reloadData()
            binding.swipeToRefresh.isRefreshing = false
        }
    }

    private fun setupUiEventsListener() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiEvent
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collectLatest {
                    when (it) {
                        is TodoListUiEvent.NavigateToEditTodoItem -> {
                            navigateToEditTodoItem(it.id)
                        }
                        is TodoListUiEvent.NavigateToNewTodoItem -> {
                            navigateToNewTodoItem()
                        }
                    }
                }
        }
    }

    private fun setupRecycler() {
        val todoItemsAdapter = TodoItemsAdapter(viewModel::onUiAction)
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        binding.todoItemsList.adapter = todoItemsAdapter
        binding.todoItemsList.layoutManager = layoutManager
        binding.todoItemsList.addItemDecoration(PreviewOffsetTodoItemDecoration(bottomOffset = 16f.toPx.toInt()))

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTodoItems()
                .flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED)
                .collectLatest {
                todoItemsAdapter.setData(it)
            }
        }
    }

    private fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }

    private fun navigateToEditTodoItem(id: String) {
        onItemClick(id, false)
    }

    private fun navigateToNewTodoItem() {
        onItemClick(generateRandomItemId(), true)
    }

    private fun onItemClick(id: String, isNewItem: Boolean) {
        dismissSnackbar()
        val action =
            TodoListFragmentDirections.actionTodoListFragmentToAddTodoItemFragment2(id, isNewItem)
        findNavController().navigate(action)
    }
}

val Number.toPx
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )