package com.easylive.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import com.easylive.component.RedisComponent;
import com.easylive.entity.constants.Constants;
import com.easylive.entity.query.VideoInfoQuery;
import com.easylive.exception.BusinessException;
import com.easylive.service.VideoInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.easylive.entity.enums.PageSize;
import com.easylive.entity.query.CategoryInfoQuery;
import com.easylive.entity.po.CategoryInfo;
import com.easylive.entity.vo.PaginationResultVO;
import com.easylive.entity.query.SimplePage;
import com.easylive.mappers.CategoryInfoMapper;
import com.easylive.service.CategoryInfoService;
import com.easylive.utils.StringTools;


/**
 * 分类信息 业务接口实现
 */
@Service("categoryInfoService")
public class CategoryInfoServiceImpl implements CategoryInfoService {
	@Resource
	private VideoInfoService videoInfoService;
	@Resource
	private CategoryInfoMapper<CategoryInfo, CategoryInfoQuery> categoryInfoMapper;
    @Autowired
    private RedisComponent redisComponent;



	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<CategoryInfo> findListByParam(CategoryInfoQuery param) {
		List<CategoryInfo> categoryInfoList = this.categoryInfoMapper.selectList(param);
		if (param.getConvert2Tree() != null && param.getConvert2Tree()) {
			categoryInfoList = convertQLine2Tree(categoryInfoList, Constants.ZERO);

		}return categoryInfoList;
	}
	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(CategoryInfoQuery param) {



		return this.categoryInfoMapper.selectCount(param);
	}
private List<CategoryInfo> convertQLine2Tree(List<CategoryInfo> dataList,Integer pid){
		List<CategoryInfo> children = new ArrayList<>();
		for(CategoryInfo m:dataList){
			if(m.getCategoryId()!=null&&m.getpCategoryId()!=null&&m.getpCategoryId().equals(pid)){
				m.setChildren(convertQLine2Tree(dataList,m.getCategoryId()));
				children.add(m);//递归调用：以当前分类 m.getCategoryId() 作为新的 pid，继续在 dataList 里找它的子分类，直到没有子分类为止。
			}
		}return children;

}
private void save2Redis(){
	CategoryInfoQuery query=new CategoryInfoQuery();
	query.setOrderBy("sort asc");
	query.setConvert2Tree(true);
	List<CategoryInfo> categoryInfoList=findListByParam(query);
	redisComponent.saveCategoryList(categoryInfoList);
}
	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<CategoryInfo> findListByPage(CategoryInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<CategoryInfo> list = this.findListByParam(param);
		PaginationResultVO<CategoryInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	@Override
	public void saveCategoryInfo(CategoryInfo bean) {
		// 通过分类编号（categoryCode）查询数据库中是否已有相同的分类信息
		CategoryInfo dbBean = this.categoryInfoMapper.selectByCategoryCode(bean.getCategoryCode());

		// 判断：如果没有分类ID且数据库中有相同编号的分类，或者有分类ID但数据库中的分类ID不相等，则抛出异常
		//ean.getCategoryId() == null：表示当前传入的分类信息 bean 是新增的，因为新增时 CategoryId 不会传递（即为 null）。
		if (bean.getCategoryId() == null && dbBean != null ||
				bean.getCategoryId() != null && dbBean != null && !bean.getCategoryId().equals(dbBean.getCategoryId())) {
			throw new BusinessException("分类编号已经存在"); // 如果分类编号已存在，抛出业务异常
		}

		// 如果没有分类ID，说明是新增分类
		if (bean.getCategoryId() == null) {
			// 查询父分类下最大排序值
			Integer maxSort = this.categoryInfoMapper.selectMaxSort(bean.getpCategoryId());
			//设置当前分类的排序值为最大排序值加 1
			bean.setSort(maxSort + 1);
			// 插入新的分类数据到数据库
			this.categoryInfoMapper.insert(bean);
		} else {
			// 如果有分类ID，说明是更新操作，根据分类ID更新分类信息
			this.categoryInfoMapper.updateByCategoryId(bean, bean.getCategoryId());
			//
		}

		// 刷新缓存，保证数据与缓存一致
		save2Redis();
	}

	@Override
	public List<CategoryInfo> getAllCategoryList() {
		List<CategoryInfo> categoryInfoList=redisComponent.getCategoryList();
		if(categoryInfoList.isEmpty()){
			save2Redis();
		}
		return redisComponent.getCategoryList();
	}

	@Override
	public void delCategory(Integer categoryId) {
		//todo 查询分类项目下是否有视频
		VideoInfoQuery videoInfoQuery = new VideoInfoQuery();
		videoInfoQuery.setCategoryIdOrPCategoryId(categoryId);
		Integer count = videoInfoService.findCountByParam(videoInfoQuery);
		if (count > 0) {
			throw new BusinessException("分类下有视频信息，无法删除");
		}
		CategoryInfoQuery categoryInfoQuery=new CategoryInfoQuery();
		categoryInfoQuery.setCategoryIdOrPCategoryId(categoryId);
		categoryInfoMapper.deleteByParam(categoryInfoQuery);
		//todo 刷新缓存
		save2Redis();
	}
//例如，如果前端发送了 categoryIds = "3,5,7"，那就意味着分类 ID 为 3 的分类应该排在最前面，接下来是 5，最后是 7。
	@Override
	public void changeSort(Integer pCategoryId, String categoryIds) {
		// 1. 把传入的分类 ID 字符串（逗号分隔）切分成数组
		//    例如 "3,5,7" -> ["3","5","7"]
		String[] categoryIdArray = categoryIds.split(",");

		// 2. 用来存放待更新的分类对象列表
		List<CategoryInfo> categoryInfoList = new ArrayList<>();

		// 3. 排序号起始值（注意：后面用的是 ++sort，所以实际从 2 开始）
		Integer sort = 0;

		// 4. 遍历每一个分类 ID
		for (String categoryId : categoryIdArray) {
			// 创建一个新的分类对象
			CategoryInfo categoryInfo = new CategoryInfo();

			// 设置分类主键 ID（当前要更新的分类）
			categoryInfo.setCategoryId(Integer.parseInt(categoryId));

			// 设置父分类 ID（说明这些分类属于哪个父类）
			categoryInfo.setpCategoryId(pCategoryId);

			// 设置排序号（每次循环 sort+1）
			// 第一次循环 sort=2，第二次=3，以此类推
			categoryInfo.setSort(++sort);

			// 把这个分类对象放到集合里
			categoryInfoList.add(categoryInfo);
		}

		// 5. 批量更新数据库，把排序号统一更新
		this.categoryInfoMapper.updateSortBatch(categoryInfoList);
		save2Redis();
	}


	/**
	 * 新增
	 */
	@Override
	public Integer add(CategoryInfo bean) {
		return this.categoryInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.categoryInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<CategoryInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.categoryInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(CategoryInfo bean, CategoryInfoQuery param) {
		StringTools.checkParam(param);
		return this.categoryInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(CategoryInfoQuery param) {
		StringTools.checkParam(param);
		return this.categoryInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据CategoryId获取对象
	 */
	@Override
	public CategoryInfo getCategoryInfoByCategoryId(Integer categoryId) {
		return this.categoryInfoMapper.selectByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryId修改
	 */
	@Override
	public Integer updateCategoryInfoByCategoryId(CategoryInfo bean, Integer categoryId) {
		return this.categoryInfoMapper.updateByCategoryId(bean, categoryId);
	}

	/**
	 * 根据CategoryId删除
	 */
	@Override
	public Integer deleteCategoryInfoByCategoryId(Integer categoryId) {
		return this.categoryInfoMapper.deleteByCategoryId(categoryId);
	}

	/**
	 * 根据CategoryCode获取对象
	 */
	@Override
	public CategoryInfo getCategoryInfoByCategoryCode(String categoryCode) {
		return this.categoryInfoMapper.selectByCategoryCode(categoryCode);
	}

	/**
	 * 根据CategoryCode修改
	 */
	@Override
	public Integer updateCategoryInfoByCategoryCode(CategoryInfo bean, String categoryCode) {
		return this.categoryInfoMapper.updateByCategoryCode(bean, categoryCode);
	}

	/**
	 * 根据CategoryCode删除
	 */
	@Override
	public Integer deleteCategoryInfoByCategoryCode(String categoryCode) {
		return this.categoryInfoMapper.deleteByCategoryCode(categoryCode);
	}
}