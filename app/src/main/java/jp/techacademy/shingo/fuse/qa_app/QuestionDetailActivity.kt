package jp.techacademy.shingo.fuse.qa_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.shingo.fuse.qa_app.databinding.ActivityQuestionDetailBinding


class QuestionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionDetailBinding

    private lateinit var question: Question
    private lateinit var adapter: QuestionDetailListAdapter
    private lateinit var answerRef: DatabaseReference
    private lateinit var favoriteRef: DatabaseReference


    private var favorite: Int = 0


    private val eventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in question.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] as? String ?: ""
            val name = map["name"] as? String ?: ""
            val uid = map["uid"] as? String ?: ""
            val favorite = map["favorite"] as? Int ?: 0


            val answer = Answer(body, name, uid, answerUid, favorite)
            question.answers.add(answer)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onChildRemoved(dataSnapshot: DataSnapshot) {}
        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {}
        override fun onCancelled(databaseError: DatabaseError) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 渡ってきたQuestionのオブジェクトを保持する
        // API33以上でgetSerializableExtra(key)が非推奨となったため処理を分岐
        @Suppress("UNCHECKED_CAST", "DEPRECATION", "DEPRECATED_SYNTAX_WITH_DEFINITELY_NOT_NULL")
        question = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getSerializableExtra("question", Question::class.java)!!
        else
            intent.getSerializableExtra("question") as? Question!!

        title = question.title


        // ListViewの準備
        adapter = QuestionDetailListAdapter(this, question)
        binding.listView.adapter = adapter
        adapter.notifyDataSetChanged()

        binding.fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる

                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", question)
                startActivity(intent)
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        answerRef = dataBaseReference.child(ContentsPATH).child(question.genre.toString())
            .child(question.questionUid).child(AnswersPATH)
        answerRef.addChildEventListener(eventListener)



        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            binding.favoriteImage.visibility = View.VISIBLE
        favoriteRef = dataBaseReference.child(FavoritePATH).child(question.uid).child(question.questionUid)

        favoriteRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                favorite = dataSnapshot.child("favorite").getValue(Int::class.java) ?: 0
                binding.favoriteImage.setImageResource(if (favorite == 1) R.drawable.ic_star else R.drawable.ic_star_border)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        binding.favoriteImage.setOnClickListener {
            if (favorite == 1) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.delete_favorite_dialog_title)
                    .setMessage(R.string.delete_favorite_dialog_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                         favorite = 0
                        binding.favoriteImage.setImageResource(if (favorite == 1) R.drawable.ic_star else R.drawable.ic_star_border)
                        saveFavoriteAddDate(question.questionUid, favorite)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .create()
                    .show()
            } else {
                val favorite = 1
                binding.favoriteImage.setImageResource(if (favorite == 1) R.drawable.ic_star else R.drawable.ic_star_border)
                saveFavoriteAddDate(question.questionUid, favorite)
            }
        }
    } else {
        // ログインしていない場合の処理
        binding.favoriteImage.visibility = View.GONE
    }
}

private fun saveFavoriteAddDate(questionUid: String, favorite: Int) {
    favoriteRef.child("favorite").setValue(favorite)
}
}






