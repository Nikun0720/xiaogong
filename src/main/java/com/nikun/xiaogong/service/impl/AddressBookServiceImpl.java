package com.nikun.xiaogong.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nikun.xiaogong.entity.AddressBook;
import com.nikun.xiaogong.mapper.AddressBookMapper;
import com.nikun.xiaogong.service.AddressBookService;
import org.springframework.stereotype.Service;

@Service
public class AddressBookServiceImpl extends ServiceImpl<AddressBookMapper, AddressBook> implements AddressBookService {
}
