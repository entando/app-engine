import React, { useState } from 'react';
import cx from 'classnames';
import { Search24 } from '@carbon/icons-react';

import './SearchBarWidget.scss';

const SearchBarWidget = ({ actionUrl, placeholder }) => {
  const [formOpened, setFormOpened] = useState(false);

  const searchFormClass = cx('SearchBar', formOpened ? 'opened' : '');

  return (
    <form action={actionUrl} className={searchFormClass}>
      {formOpened ? (
        <>
          <input type="text" name="search" placeholder={placeholder} />
          <button type="submit"><Search24 /></button>
        </>
      ) : <Search24 className="SearchBar__svg" onClick={() => setFormOpened(true)} />
      }
    </form>
  )

}

export default SearchBarWidget;