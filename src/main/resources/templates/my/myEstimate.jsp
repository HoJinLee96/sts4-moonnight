<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>견적 내역</title>
<style type="text/css">
p{
margin: 0px;
cursor: text;
}
.contentTitle{
padding-bottom : 15px;
border-bottom: 4px solid #20367a;
}
#estimateList{
margin-top: 30px;
}
.estimate{
border: 2px solid #20367a;
border-radius: 10px;
padding: 15px 15px;
margin-bottom: 15px;
position: relative;
}
.estimate p{
display: inline;
}
.status{
margin-right: 10px;
}
.name{
margin-right: 10px;
}
.phone{
margin-right: 10px;
}
#detailDiv{
display: none;
margin-top: 20px;
border-top: 2px solid #efefef;
padding-top: 20px;
}
#detailToggle{
position: absolute;
color:#20367a;
right: 15px;
top:15px;
}
#detailToggle:hover{
color:#aaa;
cursor: pointer;
}
#imagesDiv{
display: none;
margin-top: 20px;
border-top: 2px solid #efefef;
padding-top: 20px;
}
.commentList{
display:none;
border-top: 2px solid #aaa;
margin-top: 20px;
margin-bottom: 40px;
padding-top: 20px;
}
.comment{
position: relative;
border-bottom: 1px solid #aaa;
padding: 10px 5px;
}
.commentTime{
position: absolute;
right: 15px;
top: 15px;
}
.divTitle{
margin-bottom:20px;
}
pre{
display:inline-block;
word-wrap: break-word;
white-space: pre-wrap;
max-width: 950px;
cursor:text;
}
.succinctInner{
margin-bottom: 5px;
width: 950px;
overflow: hidden; 
white-space: nowrap; 
text-overflow: ellipsis; 
}
.updateButtonDiv{
position: absolute;
    right: 15px;
    bottom: 15px;
}
.updateButtonDiv button{
    border: none;
    background: none;
    cursor: pointer;
    margin-left: 10px;
    color: #20367a;
    font-size: 14px;
}
.updateButtonDiv button:hover{
color:#aaa;
}
</style>
</head>
<script type="text/javascript">
document.addEventListener("DOMContentLoaded", function() {
	var estimateList = "";
	var xhr = new XMLHttpRequest();
	xhr.open('GET','/estimate/getEstimateByUserId',true);
    xhr.onreadystatechange = function(){
    	if(xhr.readyState ===4){
    		if(xhr.status ===200){
    		    var commonEstimateDtoList = JSON.parse(xhr.responseText);
    		    commonEstimateDtoList.forEach(function(commonEstimateDto,index){
    		    	createEstimateHtml(commonEstimateDto,index);
    		    });
    			/* console.log(response); */
    		    /* var estimateDto = response[0].estimateDto; */
    		    /* createEstimateHtml(commonEstimateDtoList); */
    		    /* var imageList = JSON.parse(response.imageList);*/
    			/* console.log(estimateDto); */
    			/* console.log(imageList); */ 
    		}else{
    		}
    	}
    };
	xhr.send();
    

function createEstimateHtml(commonEstimateDto,index){
	var estimateDto = commonEstimateDto.estimateDto;
	var imageList = commonEstimateDto.imageList;
		var sortAddress = "(" + estimateDto.postcode + ") " + estimateDto.mainAddress + " " + estimateDto.detailAddress;
		var estimateDiv = $('<div class="estimate"></div>')
		.attr('id','estimate'+index);
		
		var succinctDiv = $('<div class="succinctDiv"></div>');
		var succinctDiv1 = $('<div class="succinctInner"></div>');
		var succinctDiv2 = $('<div class="succinctInner"></div>');
		var succinctDiv3 = $('<div class="succinctInner"></div>');
		var detailToggle = $('<div id="detailToggle">펼치기</div>')
		.on('click',function(){
			detailToggleAction(this);
		});
		
		var statusText = "";
		var statusColor = "";
		if (estimateDto.status === "RECEIVED") {
		    statusText = "접수";
		    statusColor = "#3030de"; 
		} else if (estimateDto.status === "IN_PROGRESS") {
		    statusText = "답변중";
		    statusColor = "red"; 
		} else if (estimateDto.status === "COMPLETED") {
		    statusText = "완료";
		    statusColor = "#33e20"; 
		} else {
		    statusText = "오류";
		    statusColor = "black";
		}
		var statusTagP = $('<p class ="status"></p>')
		    .attr('id', 'status' + index)
		    .text(statusText)
		    .css('color', statusColor); 
		    
		var nameTagP = $('<p class="name"></p>')
		.attr('id','name'+index)
		.text(estimateDto.name?estimateDto.name:'');
		var sortedAddressTagP = $('<p></p>')
		.attr('id','sortedAddress'+index)
		.text(sortAddress);
		var phoneTagP = $('<p class="phone"></p>')
		.attr('id','phone'+index)
		.text(estimateDto.phone);
		var emailTagP = $('<p></p>')
		.attr('id','email'+index)
		.text(estimateDto.email?estimateDto.email:'');
		var createTimeTagP = $('<p class="createTime"></p>')
		.attr('id','createTime'+index)
 		.text(new Date(estimateDto.createdAt).toLocaleString());
		var detailDiv = $('<div id ="detailDiv"></div>');
        var detailTitle = $('<div class="divTitle">내용</div>');
		var contentTextTagP = $('<pre></pre>')
		.attr('id','contentText'+index)
		.text(estimateDto.content?estimateDto.content:'');
		var updateButtonDiv = $('<div class="updateButtonDiv"></div>');
		var updateButton= $('<button class="updateButton">수정</button>')
		.on('click',function(){
			updateEstiamte(estimateDto);
		});
		var deleteButton= $('<button class="deleteButton">삭제</button>')
		.on('click',function(){
			deleteEstiamte(estimateDto);
		});
		var imagesDiv = $('<div id ="imagesDiv"></div>');
        var imagesTitle = $('<div class="divTitle">이미지</div>');
        imagesDiv.append(imagesTitle);
        imageList.forEach(function(image,index){
        	var img = $('<img />', {
                src: 'data:image/png;base64,' + image,  // 이미지 데이터 (Base64)
                alt: 'image_' + index,                  // 이미지 설명
                class: 'image-class',                   // CSS 클래스 (선택 사항)
                width: '100px',                         // 이미지 크기 조정 (선택 사항)
                height: '100px'                          // 이미지 크기 조정 (선택 사항)
            });

            // imagesDiv에 이미지 추가
            imagesDiv.append(img);
        });
		
			
		succinctDiv1.append(statusTagP);
		succinctDiv1.append(createTimeTagP);
		succinctDiv2.append(nameTagP);
		succinctDiv2.append(sortedAddressTagP);
		succinctDiv3.append(phoneTagP);
		succinctDiv3.append(emailTagP);

		succinctDiv.append(succinctDiv1);
		succinctDiv.append(succinctDiv2);
		succinctDiv.append(succinctDiv3);
		
		updateButtonDiv.append(updateButton);
		updateButtonDiv.append(deleteButton);

		estimateDiv.append(succinctDiv);
		estimateDiv.append(detailToggle);
		estimateDiv.append(updateButtonDiv);
		
		detailDiv.append(detailTitle);
		detailDiv.append(contentTextTagP);

		estimateDiv.append(detailDiv);
		estimateDiv.append(imagesDiv);
	    $('#estimateList').append(estimateDiv);
	    
/*         estimateDiv.find('p, #detailDiv').on('click', function(event) {
            event.stopPropagation();  // 내부 요소 클릭 시 상위 estimateDiv로 이벤트 전파를 막음
        }); */
	    
        getcommentList(estimateDto, index);
}



function detailToggleAction(div){
    var detailDiv = $(div).siblings('#detailDiv');
    var imagesDiv = $(div).siblings('#imagesDiv');
    var commentList = $(div).siblings('.commentList');
    var detailToggle = $(div).closest('#detailToggle');
    
    if (detailDiv.css('display') === 'none') {
        detailToggle.text("접기");
        detailDiv.css('display', 'block');
    	commentList.css('display', 'block');
        imagesDiv.css('display', 'block');
    } else {
        detailToggle.text("펼치기");
        detailDiv.css('display', 'none');
    	commentList.css('display', 'none');
        imagesDiv.css('display', 'none');
    }
    
/*     if (commentList.css('display') === 'none') {
        detailToggle.text("접기");
    } else {
        detailToggle.text("펼치기");
    } */
}


});

function getcommentList(estimate, index){
var commentList = "";
var xhr = new XMLHttpRequest();
xhr.open('POST','/comment/getList',true);
xhr.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
xhr.onreadystatechange = function(){
	if(xhr.readyState ===4){
		if(xhr.status ===200){
		    commentList = JSON.parse(xhr.responseText); 
            var estimateDiv = $('#estimate' + index);
            appendCommentList(estimateDiv, commentList); 
		}else{
		}
	}
};
xhr.send(JSON.stringify(estimate.estimateId));
}

function appendCommentList(estimateDiv, commentList) {
    var commentDiv = $('<div class="commentList"></div>');
    var commentTitle = $('<div class="divTitle">답글</div>');
    commentDiv.append(commentTitle);
    
    commentList.forEach(function(comment, index) {
        var commentItem = $('<div class="comment"></div>');
        var commentText = $('<pre></pre>')
            .attr('class', 'commentText')
            .text(comment.commentText);
        var commentTime = $('<p></p>')
            .attr('class', 'commentTime')
            .text(new Date(comment.createdAt).toLocaleString());
        

        commentItem.append(commentText);
        commentItem.append(commentTime);

        commentDiv.append(commentItem);
    });

    estimateDiv.append(commentDiv);
}
    
function deleteEstiamte(estimate){
	var xhr = new XMLHttpRequest();
	xhr.open('POST','/estimate/delete',true);
	xhr.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
	xhr.onreadystatechange = function(){
		if(xhr.readyState ===4){
			if(xhr.status ===200){
				alert("접수 내역을 삭제하였습니다.");
			}else{
			}
		}
	};
	xhr.send(JSON.stringify(estimate.estimateId));
	}

</script>
<body>
<%@ include file ="/WEB-INF/view/main/main_header.jsp" %>
	<div class="container">
		<%@ include file="mypageSidebar.jsp"%>
		<div class="content">
			<p class="headerFont contentTitle">견적 내역</p>
			<div id ="estimateList">

			</div>
		</div>
	</div>
<%@ include file ="/WEB-INF/view/main/main_footer.jsp" %>
</body>
<script type="text/javascript">

</script>
</html>