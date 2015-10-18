package net.ibaixin.chat.model;

import java.io.Serializable;
/**
 * 评论实体
 * @author DDJ
 *
 */
public class Comment implements Serializable {
	private static final long serialVersionUID = 1L;
	/** 评论内容 */
	private String content;
	/** 评论时间 */
	private String commentTime;
	/** 评论用户 */
	private String commentUser;
	/** 段子Id */
	private Integer jokeId;
	
	protected Integer id ;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getCommentTime() {
		return commentTime;
	}

	public void setCommentTime(String commentTime) {
		this.commentTime = commentTime;
	}

	public String getCommentUser() {
		return commentUser;
	}

	public void setCommentUser(String commentUser) {
		this.commentUser = commentUser;
	}

	public Integer getJokeId() {
		return jokeId;
	}

	public void setJokeId(Integer jokeId) {
		this.jokeId = jokeId;
	}

	public Comment(String content, String commentTime, String commentUser,
			Integer jokeId) {
		super();
		this.content = content;
		this.commentTime = commentTime;
		this.commentUser = commentUser;
		this.jokeId = jokeId;
	}

	public Comment() {
		super();
	}

	@Override
	public String toString() {
		return "Comment [content=" + content + ", commentTime=" + commentTime
				+ ", commentUser=" + commentUser + ", jokeId=" + jokeId
				+ ", id=" + id + "]";
	}
	
}
