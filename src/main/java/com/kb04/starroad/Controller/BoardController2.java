package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.board.BoardRequestDto;
import com.kb04.starroad.Dto.board.BoardResponseDto;
import com.kb04.starroad.Entity.Board;
import com.kb04.starroad.Service.BoardService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
public class BoardController2 {

    @Autowired
    private BoardService2 boardService;
    @GetMapping("/starroad/board/write")
    public ModelAndView board() {
        ModelAndView mav = new ModelAndView("board/write");
        return mav;
    }
    @GetMapping("/starroad/board/main")
    public ModelAndView boardMain() {
        ModelAndView mav = new ModelAndView("board/main");
        return mav;
    }

    //자유게시판,인증게시판 요청
    @GetMapping("/starroad/board/free")
    public ModelAndView boardList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size,
            @RequestParam(name = "type", defaultValue = "F") String type,
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("board/board");

        // 페이징 정보 설정
        PageRequest pageable = PageRequest.of(page, size, Sort.by("regdate").descending());



        Page<Board> boardPage;

        if ("F".equals(type)) {
            // type이 "F"인 경우 자유게시판 목록 조회
            boardPage = boardService.findPaginated(pageable);
        } else if ("C".equals(type)) {
            // type이 "C"인 경우 인증방 목록 조회
            boardPage = boardService.findAuthenticatedPaginated(pageable);
        }
        else {
            // 잘못된 type 값이 들어온 경우 예외 처리
            throw new IllegalArgumentException("잘못된 type 값입니다.");
        }

        mav.addObject("freeBoardPage", boardPage);
        mav.addObject("type", type); // View에서 현재 type 값을 사용할 수 있도록 추가

        return mav;
    }

    //인기게시판 요청
    @GetMapping("/starroad/board/popular")
    public ModelAndView popularBoardList(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "6") int size,
            HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("board/board");

        // 페이징 정보 설정
        PageRequest pageable = PageRequest.of(page, size, Sort.by("likes").descending());

        // 게시글 목록 조회 (likes 내림차순)
        Page<Board> boardPage = boardService.getPopularBoards(pageable);
        //웹 페이지로 데이터를 전달하기 위한 객체로, 이를 통해 뷰(HTML 템플릿)로 데이터를 전송
        mav.addObject("popularBoardPage", boardPage);
        //ModelAndView에 "type"이라는 키를 사용하여 "popular" 문자열을 추가
        mav.addObject("type", "popular"); // 인기 게시판임을 표시하기 위한 값

        return mav;
        //인기 게시글을 조회하여 해당 게시글 목록을 boardPage에 저장하고,
        // 이를 ModelAndView에 추가하여 뷰로 전달하는 역할
    }

    @GetMapping("/starroad/board/update")
    public ModelAndView updateBoard(@RequestParam("no") Integer no) {

        ModelAndView mav = new ModelAndView("board/update");

        Optional<Board> boardOptional = boardService.findById(no);


        Board board = boardOptional.get();
        BoardResponseDto boardResponseDto = board.toBoardResponseDto();

        mav.addObject("board", boardResponseDto);

        return mav;

    }
    @PostMapping("/starroad/board/updatePro")
    public ModelAndView updateAction(@RequestParam("no") Integer no,
                                     @RequestParam("title") String title,
                                     @RequestParam("content") String content,
                                     @RequestParam(value = "image", required = false) MultipartFile imageFile) {



        ModelAndView mav = new ModelAndView("board/update");


        // 5. 수정된 게시글 상세보기 페이지로 이동
        mav.setViewName("redirect:/starroad/board/detail?no=" + no);

        return mav;

    }



    @PostMapping("/starroad/board/writepro")
    public ResponseEntity<String> boardWritePro(
            @RequestParam("type") String type,
            @RequestParam("detailType") String detailType,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("image") MultipartFile imageFile
    ) {
        try {
            boardService.writeBoard(type, detailType, title, content, imageFile);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Location", "/starroad/board/main");
            return new ResponseEntity<>("", headers, HttpStatus.FOUND);
        } catch (IOException e) {
            e.printStackTrace();
            // 이미지 업로드 실패 시 에러 응답
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Image upload failed.");
        }
    }
}