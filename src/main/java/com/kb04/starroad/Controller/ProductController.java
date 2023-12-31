package com.kb04.starroad.Controller;

import com.kb04.starroad.Dto.*;
import com.kb04.starroad.Dto.product.BaseRateDto;
import com.kb04.starroad.Dto.product.ConditionDto;
import com.kb04.starroad.Dto.product.ProductResponseDto;
import com.kb04.starroad.Service.ProductService;
import io.swagger.annotations.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = {"예적금 상품 API"})
@RestController
public class ProductController {
    private final ProductService productService;

    private static final int ITEMS_PER_PAGE = 3;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @ApiOperation(value = "예적금 상품 조회", notes = "예적금 상품을 조회할 수 있다")
    @GetMapping("/starroad/product")
    public ModelAndView product(
            Model model,
            @ApiParam(value = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") int page,
            HttpServletRequest request) {
        MemberDto member = getLoginMember(request);
        List<ProductResponseDto> productList = null;
        // 로그인 안한 경우 첫 페이지
        if (member == null) {
            model.addAttribute("user", null);
            productList = productService.getProductList();
        } else { // 로그인 한 경우 첫 페이지
            MemberDto loginMember = member;
            Double monthlyAvailablePrice = getMonthlyAvailablePricePerMember(loginMember);
            setUserInfoInModel(model, loginMember, monthlyAvailablePrice);
            productList = productService.getProductList(monthlyAvailablePrice);
        }
        setPageIndexInModel(model, page, productList);
        ModelAndView mav = new ModelAndView("product/product");
        return mav;
    }

    @ApiOperation(value = "예적금 상품 검색", notes = "예적금 상품을 검색할 수 있다")
    @GetMapping("/starroad/product/result")
    public ModelAndView product_search_result(
            Model model,
            @ApiParam(value = "상품 유형", example = "S") @RequestParam(required = false) String type,
            @ApiParam(value = "최대 가능 가입 기간", example = "36") @RequestParam(required = false) String period,
            @ApiParam(value = "이자 과세") @RequestParam(required = false) String rate,
            @ApiParam(value = "상품명", example = "KB") @RequestParam(required = false) String query,
            @ApiParam(value = "페이지 번호", example = "1") @RequestParam(defaultValue = "1") int page,
            HttpServletRequest request) {

        MemberDto member = getLoginMember(request);
        List<ProductResponseDto> productList = null;
        // 로그인 안한 경우 검색
        if (member == null) {
            model.addAttribute("user", null);
            if (type != null || period != null || query != null) {
                productList = productService.findByForm(type.charAt(0), period, query);
            } else {
                productList = productService.getProductList();
            }
        } else { // 로그인 한 경우 검색
            MemberDto loginMember = member;
            Double monthlyAvailablePrice = getMonthlyAvailablePricePerMember(loginMember);
            if (type != null || period != null || query != null) {
                productList = productService.findByFormAndMember(type.charAt(0), period, query, monthlyAvailablePrice);
            } else {
                productList = productService.getProductList(monthlyAvailablePrice);
            }
            setUserInfoInModel(model, loginMember, monthlyAvailablePrice);
        }

        if (type != null)
            model.addAttribute("type", type);
        if (period != null) {
            model.addAttribute("period", period);
            productList = setBaseRate(productList, Integer.parseInt(period));
        }
        if (rate != null) {
            model.addAttribute("rate", rate);
            if (rate.equals("base"))
                model.addAttribute("rate_value", 0.154);
            else
                model.addAttribute("rate_value", 0.0);
        }
        if (query != null)
            model.addAttribute("query", query);

        setPageIndexInModel(model, page, productList);
        ModelAndView mav = new ModelAndView("product/product_result");
        return mav;
    }

    private static MemberDto getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession();
        MemberDto loginMember = (MemberDto) session.getAttribute("currentUser");
        return loginMember;
    }

    private Double getMonthlyAvailablePricePerMember(MemberDto loginMember) {
        int memberSalary = loginMember.getSalary();
        int memberGoal = loginMember.getGoal();
        Double monthGoal = (1.0 * memberSalary) * (1.0 * memberGoal) / 100;

        List<SubscriptionDto> subscriptionList = productService.getSubscriptions(loginMember);
        int sum = 0; // 매달 이미 나가고 있는 적금의 양
        for (SubscriptionDto dto : subscriptionList) {
            if (dto.getProd().getType() == 'S') // 적금인 상품에 대해서 매달 나가는 비용 계산
                sum += dto.getPrice();
        }
        Double result = monthGoal - sum;

        return result;
    }

    // 검색했을 때 기간 적용 -> 최대 기본 이율
    private List<ProductResponseDto> setBaseRate(List<ProductResponseDto> productList, int period) {
        List<BaseRateDto> baseRates = productService.getBaseRates(period);
//        System.out.println(baseRates);
        for (BaseRateDto dto : baseRates){
            for (ProductResponseDto prodDto: productList) {
                if(prodDto.getNo() == dto.getProd().getNo())
                    prodDto.setBaseRate(dto.getRate());
            }
        }
        return productList;
    }

    private List<ProductResponseDto> setEstimatedAmount(List<ProductResponseDto> productList, Double monthlyAvailablePrice) {

        return productList;
    }

    private void setPageIndexInModel(Model model, @RequestParam(defaultValue = "1") int page, List<ProductResponseDto> productList) {
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, productList.size());

        model.addAttribute("productItems", productList.subList(startIndex, endIndex));
        model.addAttribute("pageEndIndex", Math.ceil(productList.size() / Double.valueOf(ITEMS_PER_PAGE)));
        model.addAttribute("currentPage", page);
    }

    private void setUserInfoInModel(Model model, MemberDto loginMember, Double monthlyAvailablePrice) {
        List<ConditionDto> memberConditions = productService.getMemberConditions(loginMember);
        Map<Integer, Double> groupedData = new HashMap<>();
        for (ConditionDto condition : memberConditions) {
            int prodNo = condition.getProd().getNo();
            double conditionRate = condition.getRate();
            groupedData.put(prodNo, groupedData.getOrDefault(prodNo, 0.0) + conditionRate);
        }
        model.addAttribute("user", loginMember.getName());
        model.addAttribute("monthlyAvailablePrice", monthlyAvailablePrice);
        model.addAttribute("memberConditionRates", groupedData);
    }
}
