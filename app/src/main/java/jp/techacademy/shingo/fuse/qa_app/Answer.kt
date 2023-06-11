package jp.techacademy.shingo.fuse.qa_app


import java.io.Serializable

class Answer(val body: String, val name: String, val uid: String, val answerUid: String,favorite:Int) :
    Serializable {}