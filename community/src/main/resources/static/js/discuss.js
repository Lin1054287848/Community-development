$(function (){
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});

function like(btn, entityType, entityId, entityUserId, postId) {
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId, "entityUserId":entityUserId, "postId":postId},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $(btn).children("i").text(data.likeCount); //btn 获取按钮的节点 ，获取子节点 i -> <i> 1 </i> ，然后改变其内容
                $(btn).children("b").text(data.likeStatus==1?'已赞':"赞");//判断点赞的状态 ，如状态为1表示点赞了，显示已赞；否则显示赞
            } else {
                alert(data.msg);
            }
        }
    );
}

//置顶
function setTop() {
    $.post(
        CONTEXT_PATH + "/discuss/top",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $("#topBtn").text(data.type==1?'取消置顶':'置顶');
            } else {
                alert(data.msg);
            }
        }
    );
}

//加精
function setWonderful() {
    $.post(
        CONTEXT_PATH + "/discuss/wonderful",
        {"id":$("#postId").val()},
        function(data) {
            data = $.parseJSON(data);
            if(data.code == 0) {
                $("#wonderfulBtn").text(data.status==1?'取消加精':'加精');
            } else {
                alert(data.msg);
            }
        }
    );
}

//删除
function setDelete(){
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"id":$("#postId").val()},
        function (data){
            data = $.parseJSON(data);
            if(data.code == 0){
                location.href = CONTEXT_PATH + "/index";
            }else{
                alert(data.msg);
            }
        }
    );
}