package com.leidos.bmech.gui;

public class Task {
	private TaskType	taskType;
	private Object		target;

	public Task(TaskType task, Object target) {
		this.target = target;
		this.taskType = task;
	}

	public enum TaskType {
		ADD_WS, DEL_WS
	}

	public TaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	@Override
	public String toString() {
		return "Task [taskType=" + taskType.name() + ", target=" + target + "]";
	}

}
