package com.datapipeline.agent.entity

enum class DpDataTaskState {
    IMPORTING,
    ACTIVATING,
    ACTIVE,
    SUSPENDING,
    SUSPEND,
    DELETING,
    DELETED,
    WAITING_AUTO_RESTART,
    WAITING_CACHE_CLEAR,
    WAITING_START,
    WAITING_RESOURCE,
    FINISHED,
    ERROR
}