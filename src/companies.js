import {inject} from 'aurelia-framework';
import {HttpClient, json} from 'aurelia-fetch-client';
import {host} from 'host';
import {Router} from 'aurelia-router';
import ultimatePagination from 'ultimate-pagination';

@inject(Element, HttpClient, host, Router)

export class Companies {
  companies = null;
  text = 'simple';
  name = '';
  page = 1;
  page_size = 10;
  tableTitle = "Top Companii";
  description = "2015 - 2016";
  iconClass = "";
  head = true;
  headers = ['IDNO', 'Denumirea', 'Data de inregistrare', 'Suma contractelor (MDL)'];
  lastQuery = '';

  constructor(element, http, host, router) {
    this.element = element;
    this.http = http;
    this.host = host;
    this.router = router;


  }

  activate() {
    var url = this.lastQuery = this.host + '/company/top?by=totalSum&limit=10&page=';
    this.getCompaniesByQuery(url, 1);
  }
  search() {

    this.description = 'conform căutării  - ' + this.name;

    this.name = this.name.normalize('NFC');

    if(this.name && this.name.length > 0) {
      this.companies = null;
      var url = this.lastQuery = this.host + '/company/search?sorted_by=name&limit=' + this.page_size + '&text=' + this.name + '&page=' ;
      this.getCompaniesByQuery(url, 1);
      this.tableTitle = 'Rezultate';
    }
  }

  getCompaniesByQuery(url, page) {
    return this.http.fetch(url + page)
      .then(resp => resp.json())
      .then(data => {
        this.companies = data.content;
        this.companies.map(el => el.newId = el.id.substring(1));
        this.currentPage = page;
        this.totalPages = data.totalPages;
        if (data.totalPages > 1) {
          this.drawPagination();
        } else {
          this.pagination = null;
        }
      }).catch(e => {
        console.log(e);
        this.companies = [];
      });
  }

  drawPagination() {

    this.pagination = ultimatePagination.getPaginationModel({
      currentPage: this.currentPage,
      totalPages: this.totalPages
    });

    this.pagination.map(item => {
      switch (item.type) {
        case 'PAGE':
          item.label = item.value;
          break;
        case 'ELLIPSIS':
          item.label = '...';
          break;
        case 'FIRST_PAGE_LINK':
          item.hide = true;
          break;
        case 'LAST_PAGE_LINK':
          item.hide = true;
          break;
        case 'PREVIOS_PAGE_LINK':
          item.prev = true;
          break;
        case 'NEXT_PAGE_LINK':
          item.next = true;
          break;
      }
    });

  }

  loadPage(value) {
    this.getCompaniesByQuery(this.lastQuery, value);
  }
}
