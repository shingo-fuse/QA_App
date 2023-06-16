package jp.techacademy.shingo.fuse.qa_app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import jp.techacademy.shingo.fuse.qa_app.databinding.ActivityQuestionDetailBinding


class QuestionDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuestionDetailBinding

    private lateinit var question: Question
    private lateinit var adapter: QuestionDetailListAdapter
    private lateinit var answerRef: DatabaseReference
    private lateinit var favoriteRef: DatabaseReference

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
            val answer = Answer(body, name, uid, answerUid)
            question.answers.add(answer)
            adapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<*, *>

            // 変更があったQuestionを探す
            if (dataSnapshot.key.equals(question.questionUid)) {
                // このアプリで変更がある可能性があるのは回答（Answer)のみ
                question.answers.clear()
                val answerMap = map["answers"] as Map<*, *>?
                if (answerMap != null) {
                    for (key in answerMap.keys) {
                        val map1 = answerMap[key] as Map<*, *>
                        val map1Body = map1["body"] as? String ?: ""
                        val map1Name = map1["name"] as? String ?: ""
                        val map1Uid = map1["uid"] as? String ?: ""
                        val map1AnswerUid = key as? String ?: ""
                        val answer =
                            Answer(map1Body, map1Name, map1Uid, map1AnswerUid)
                        question.answers.add(answer)
                    }


                    adapter.notifyDataSetChanged()
                }
            }

        }

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

            favoriteRef=
            dataBaseReference.child(FavoritePATH).child(user.uid).child(question.questionUid)

            favoriteSearch()

            binding.favoriteImage.setOnClickListener {
                favoriteRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            AlertDialog.Builder(this@QuestionDetailActivity)
                                .setTitle(R.string.delete_favorite_dialog_title)
                                .setMessage(R.string.delete_favorite_dialog_message)
                                .setPositiveButton(android.R.string.ok) { _, _ ->
                                    favoriteRef.removeValue()
                                    favoriteSearch()
                                } .setNegativeButton(android.R.string.cancel) { _, _ -> }
                                .create()
                                .show()
                        } else {
                            favoriteRef.child("genre").setValue(question.genre.toString())

                            favoriteSearch()
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
            } } else {
            // ログインしていない場合の処理
            binding.favoriteImage.visibility = View.GONE
        }
    }

    private  fun favoriteSearch(){
        favoriteRef.child("genre").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    binding.favoriteImage.setImageResource(R.drawable.ic_star)
                } else {
                    binding.favoriteImage.setImageResource(R.drawable.ic_star_border)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }
}






