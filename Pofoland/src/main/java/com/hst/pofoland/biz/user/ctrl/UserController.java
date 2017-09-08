package com.hst.pofoland.biz.user.ctrl;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.social.connect.Connection;
import org.springframework.social.google.api.Google;
import org.springframework.social.google.api.impl.GoogleTemplate;
import org.springframework.social.google.api.plus.Person;
import org.springframework.social.google.api.plus.PlusOperations;
import org.springframework.social.google.connect.GoogleConnectionFactory;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.GrantType;
import org.springframework.social.oauth2.OAuth2Operations;
import org.springframework.social.oauth2.OAuth2Parameters;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.hst.pofoland.biz.category.service.impl.CategoryServiceImpl;
import com.hst.pofoland.biz.category.vo.CategoryVO;
import com.hst.pofoland.biz.user.service.impl.UserServiceImpl;
import com.hst.pofoland.biz.user.vo.UserVO;
import com.hst.pofoland.common.auth.Ase128Encrypt;
import com.hst.pofoland.common.auth.GoogleAuthentication;
import com.hst.pofoland.common.constnat.NetworkConstant;
import com.hst.pofoland.common.utils.LoggerManager;
import com.hst.pofoland.common.vo.ResponseVO;

/**
 * 
 * 시스템명 : 포트폴리오 관리 시스템
 * $com.hst.pofoland.biz.user.UserController.java
 * 클래스 설명 : User관련 Controller
 *
 * @author 김영훈
 * @since 2017. 7. 27.
 * @version 1.0.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 *
 * 수정일			수정자			수정내용
 * -------------------------------------------------
 * 2017. 7. 27.		김영훈			최초생성
 * </pre>
 */

@Controller
public class UserController implements InitializingBean{
	
	@Inject
	UserServiceImpl userService;
	
	@Inject
	Ase128Encrypt ase128Encrypt;
	
	@Inject
	CategoryServiceImpl categoryService;
	
	@Inject
	GoogleAuthentication googleAuth;
	
	GoogleConnectionFactory googleConnectionFactory = null;
	OAuth2Parameters googleOAuth2Parameters = null;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		googleConnectionFactory = googleAuth.getGoogleConnectionFactory();
		googleOAuth2Parameters = googleAuth.getOauth2Parameter();
	}
	
	/**
	 * 유저 회원가입
	 * @param userVO
	 * @return
	 */
	@RequestMapping(value="/user" , method=RequestMethod.POST)
	@ResponseBody
	public ResponseVO createUser(@ModelAttribute UserVO userVO , HttpServletResponse response) {
		
		userVO = userService.createUser(userVO);
		ResponseVO responseVO = new ResponseVO();
		
		if (userVO.getUserSeq() > 0) {
			responseVO.setCode(NetworkConstant.COMMUNICATION_SUCCESS_CODE);
			responseVO.setData(userVO);
		}
		
		return responseVO;
	}
	
	
	/**
	 * 구글 가입 화면 추출
	 * @param response
	 */
	@RequestMapping(value="/google/login", method=RequestMethod.GET)
	public void googleLogin(HttpServletResponse response) {
		
		OAuth2Operations oauthOperations = googleConnectionFactory.getOAuthOperations();
		String url = oauthOperations.buildAuthenticateUrl(GrantType.AUTHORIZATION_CODE, googleOAuth2Parameters);
		
		try {
			response.sendRedirect(url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 구글 사용자 인증 확인
	 * @param request
	 * @return
	 * @throws IOException 
	 */
	@RequestMapping(value="/google/user", method=RequestMethod.GET)
	public ModelAndView googleLoginCheck (HttpServletRequest request , HttpServletResponse response) throws IOException {
		
		ModelAndView mav = new ModelAndView();
		String code = request.getParameter("code");
		
		OAuth2Operations oauthOperations = googleConnectionFactory.getOAuthOperations();
		AccessGrant accessGrant = oauthOperations.exchangeForAccess(code , googleOAuth2Parameters.getRedirectUri(), null);
		
		String accessToken = accessGrant.getAccessToken();
		Long expireTime = accessGrant.getExpireTime();
		if (expireTime != null && expireTime < System.currentTimeMillis()) {
			accessToken = accessGrant.getRefreshToken();
			LoggerManager.info(getClass(),"accessToken is expired. refresh token = {}", accessToken);
		}
		Connection<Google> connection = googleConnectionFactory.createConnection(accessGrant);
		Google google = connection == null ? new GoogleTemplate(accessToken) : connection.getApi();
		
		PlusOperations plusOperations = google.plusOperations();
		Person person = plusOperations.getGoogleProfile();
		String googleId = person.getAccountEmail();
		
		Integer userSeq = userService.seqSearchUser(googleId);
		
		if (userSeq == null || userSeq < 0) {
			HttpSession session = request.getSession();
			
			UserVO userVO = new UserVO();
			userVO.setUserId(googleId);
			session.setAttribute("user",userVO);
			
			mav.setViewName("user/joinStep1");
			mav.addObject("type", "google");
		} else {
			UserVO userVO = userService.searchUser(userSeq);
			userVO.setUserProfileUrl(person.getImageUrl());
			
			HttpSession session = request.getSession();
			session.setAttribute("user", userVO);
			
			response.sendRedirect("/home");
		}
		
		return mav;
	}
	
	
	/**
	 * 유저 아이디중복확인
	 * @param userId
	 * @return
	 */
	@RequestMapping(value="/user/checkid/{userId}", method=RequestMethod.GET)
	@ResponseBody
	public ResponseVO duplicateCheckId(@PathVariable String userId) {
		System.out.println("userId: " + userId);
		String checkId = userService.duplicateCheckId(userId);
		
		ResponseVO responseVO = new ResponseVO();
		if (checkId != null) {
			responseVO.setCode(NetworkConstant.COMMUNICATION_SUCCESS_CODE);
		}
		
		return responseVO;
	}
	
	/**
	 * 유저 닉네임 중복확인
	 * @param userNick
	 * @return
	 */
	@RequestMapping(value="/user/checknick/{userNick}", method=RequestMethod.GET)
	@ResponseBody
	public ResponseVO duplicateCheckNick(@PathVariable String userNick) {
		
		String checkNick = userService.duplicateCheckNick(userNick);
		
		ResponseVO responseVO = new ResponseVO();
		if (checkNick != null) {
			responseVO.setCode(NetworkConstant.COMMUNICATION_SUCCESS_CODE);
		}
		
		return responseVO;
	}
	
	/**
	 * 유저 이메일 중복확인
	 * @param userEmail
	 * @return
	 */
	@RequestMapping(value="/user/checkemail", method=RequestMethod.GET)
	@ResponseBody
	public ResponseVO duplicateCheckEmail(String userEmail) {
		
		String checkEmail = userService.duplicateCheckEmail(userEmail);
		
		ResponseVO responseVO = new ResponseVO();
		if (checkEmail != null) {
			responseVO.setCode(NetworkConstant.COMMUNICATION_SUCCESS_CODE);
		}
		
		return responseVO;
	}
	
	/**
	 * 유저 정보조회
	 * @param userSeq
	 * @return
	 */
	@RequestMapping(value="/user/{userSeq}" , method=RequestMethod.GET)
	@ResponseBody
	public ResponseVO searchUser(@PathVariable Integer userSeq) {
		
		UserVO userVO = userService.searchUser(userSeq);
		
		ResponseVO responseVO = new ResponseVO();
		responseVO.setData(userVO);

		if (userVO != null) {
			responseVO.setCode(NetworkConstant.COMMUNICATION_SUCCESS_CODE);
		}
		
		return responseVO;
	}
	
	/**
	 * 유저 메일인증처리
	 * @param userAuthKey
	 * @param userSeq
	 * @return
	 */
	@RequestMapping(value="/user/{userSeq}/auth/{userAuthKey}" , method=RequestMethod.GET)
	public ModelAndView authProcessUser(@PathVariable String userAuthKey ,@PathVariable Integer userSeq) {
		
		UserVO userVO = new UserVO();
		ase128Encrypt.createEncryptKey(NetworkConstant.ENCRYPTION_MAILAUTH_KEY);
		userAuthKey = ase128Encrypt.decode(userAuthKey);
		userVO.setUserAuthKey(userAuthKey);
		userVO.setUserSeq(userSeq);
		
		int code = userService.authProcessUser(userVO);
		
		ModelAndView mav = new ModelAndView("/user/mailAuthResult");
		
		if (code == NetworkConstant.COMMUNICATION_SUCCESS_CODE) {
			mav.addObject("resultMsg", "메일 인증에 성공하셨습니다. 계속하여 가입해주세요.!");
			
		} else {
			mav.addObject("resultMsg", "메일 인증에 실패하셨씁니다. 다시 시도하여 주세요.!");
		}
		mav.addObject("code", code);
		
		return mav;
	}
	
	
	/**
	 * 유저 인증상태 확인
	 * @param userSeq
	 * @return
	 */
	@RequestMapping(value="/user/checkauth/{userSeq}", method=RequestMethod.GET)
	@ResponseBody
	public ResponseVO authCheckUser(@PathVariable Integer userSeq) {
		
		UserVO userVO = userService.authCheckUser(userSeq);
		char userAuthYn = userVO.getUserAuthYn();
		ResponseVO responseVO = new ResponseVO();
		if(userAuthYn == 'Y') {
			responseVO.setCode(NetworkConstant.COMMUNICATION_SUCCESS_CODE);
			responseVO.setData(userVO);
		} 
		
		return responseVO;
	}
	
	/**
	 * 추가정보 입력 페이지 이동
	 * @return
	 */
	@RequestMapping(value="/user/addInfo/{userSeq}", method=RequestMethod.GET)
	public ModelAndView addInfoPage(@PathVariable Integer userSeq) {
		
		List<CategoryVO> categoryList = categoryService.getJobCategoryList();
		
		ModelAndView mav = new ModelAndView("user/joinStep3");
		
		mav.addObject("jobList", categoryList);
		mav.addObject("userSeq", userSeq);
		
		return mav;
	}
	
	/**
	 * 추가정보 등록
	 * @param userVO
	 * @return
	 */
	@RequestMapping(value="/user/addinfo", method=RequestMethod.POST)
	@ResponseBody
	public ResponseVO addInfoUser(@ModelAttribute UserVO userVO) {
		
		LoggerManager.info(getClass(), "in {}", userVO.toString());
		
		boolean nickResult = userService.addInfoUser(userVO);
		
		ResponseVO responseVO = new ResponseVO();
		if (nickResult) {
			responseVO.setCode(NetworkConstant.COMMUNICATION_SUCCESS_CODE);
		}
		
		return responseVO;
	}
	
}
